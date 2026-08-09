package com.bikepooling.service;

import com.bikepooling.config.OsrmClient;
import com.bikepooling.dto.request.ApplyScheduledRideRequest;
import com.bikepooling.dto.response.ScheduledRideApplicantResponse;
import com.bikepooling.dto.response.ScheduledRideApplicationDayResponse;
import com.bikepooling.entity.*;
import com.bikepooling.enums.ApplicationStatus;
import com.bikepooling.enums.RideState;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.*;
import com.bikepooling.util.FareUtil;
import com.bikepooling.util.RouteMatchUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledRideApplicationService {

    private final ScheduledRideTemplateRepository templateRepo;
    private final ScheduledRideInstanceRepository instanceRepo;
    private final ScheduledRideApplicationRepository applicationRepo;
    private final ScheduledRideApplicationDayRepository appDayRepo;
    private final UserRepository userRepo;
    private final OsrmClient osrmClient;
    private final AppConfigService configService;
    private final FcmService fcmService;
    private final ApplicationEventPublisher eventPublisher;

    private static final List<ApplicationStatus> ACTIVE_STATUSES =
            List.of(ApplicationStatus.PENDING, ApplicationStatus.CONFIRMED);

    // ── Apply ─────────────────────────────────────────────────────────────────

    @Transactional
    public List<ScheduledRideApplicationDayResponse> apply(
            Long templateId, ApplyScheduledRideRequest req, Long bookerId) {

        ScheduledRideTemplate template = templateRepo.findByIdWithDetails(templateId)
                .orElseThrow(() -> AppException.notFound("Scheduled ride not found"));

        if (template.getPostedBy().getId().equals(bookerId)) {
            throw AppException.badRequest("You cannot apply to your own scheduled ride.");
        }
        if (req.getPickupLat().compareTo(req.getDropLat()) == 0
                && req.getPickupLng().compareTo(req.getDropLng()) == 0) {
            throw AppException.badRequest("Pickup and drop location cannot be the same.");
        }

        User booker = userRepo.findById(bookerId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        RouteMatchUtil.MatchResult stage1 = RouteMatchUtil.evaluateStage1(
                template.getFromLat().doubleValue(), template.getFromLng().doubleValue(),
                template.getToLat().doubleValue(),   template.getToLng().doubleValue(),
                template.getDistanceKm().doubleValue(),
                req.getPickupLat().doubleValue(), req.getPickupLng().doubleValue(),
                req.getDropLat().doubleValue(),   req.getDropLng().doubleValue()
        );
        if (!stage1.isMatched()) {
            throw AppException.badRequest(stage1.getReason());
        }

        OsrmClient.RouteLegs legs = osrmClient.getRouteLegs(
                new double[]{template.getFromLat().doubleValue(), req.getPickupLat().doubleValue(),
                        req.getDropLat().doubleValue(), template.getToLat().doubleValue()},
                new double[]{template.getFromLng().doubleValue(), req.getPickupLng().doubleValue(),
                        req.getDropLng().doubleValue(), template.getToLng().doubleValue()});

        double detourKm = Math.max(legs.getTotalKm() - template.getDistanceKm().doubleValue(), 0.0);
        RouteMatchUtil.MatchResult stage2 = RouteMatchUtil.checkDetourBudget(
                detourKm, template.getExtraDistanceKm().doubleValue());
        if (!stage2.isMatched()) {
            throw AppException.badRequest(stage2.getReason());
        }

        double bookerRoadKm = legs.getLeg(1);
        BigDecimal bookerDistanceKm = BigDecimal.valueOf(bookerRoadKm).setScale(2, RoundingMode.HALF_UP);

        BigDecimal bookerFare = FareUtil.calculateBookerFare(
                bookerRoadKm, template.getDistanceKm().doubleValue(),
                template.getFare(), configService.getMinFare());

        List<ScheduledRideInstance> instances = new ArrayList<>();
        for (LocalDate date : req.getDates()) {
            ScheduledRideInstance inst = instanceRepo.findByTemplateIdAndRideDate(templateId, date)
                    .orElseThrow(() -> AppException.notFound(
                            "No ride available on " + date + " for this scheduled ride."));
            if (inst.getState() != RideState.OPEN) {
                throw AppException.conflict(
                        date + " is not open for applications. State: " + inst.getState());
            }
            instances.add(inst);
        }

        ScheduledRideApplication application = ScheduledRideApplication.builder()
                .template(template)
                .booker(booker)
                .pickupName(req.getPickupName())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .dropName(req.getDropName())
                .dropLat(req.getDropLat())
                .dropLng(req.getDropLng())
                .note(req.getNote())
                .bookerDistanceKm(bookerDistanceKm)
                .bookerFare(bookerFare)
                .build();
        application = applicationRepo.save(application);

        List<ScheduledRideApplicationDayResponse> responses = new ArrayList<>();
        for (ScheduledRideInstance inst : instances) {
            ScheduledRideApplicationDay day = ScheduledRideApplicationDay.builder()
                    .application(application)
                    .instance(inst)
                    .status(ApplicationStatus.PENDING)
                    .build();
            day = appDayRepo.save(day);
            responses.add(ScheduledRideApplicationDayResponse.from(day));

            fcmService.notifyDriverScheduledApplicant(
                    template.getPostedBy().getId(), booker.getFullName(),
                    templateId, inst.getRideDate());
        }

        log.info("Scheduled application submitted: templateId={} bookerId={} days={}",
                templateId, bookerId, req.getDates());

        return responses;
    }

    // ── Confirm one day ─────────────────────────────────────────────────────

    @Transactional
    public void confirmDay(Long applicationDayId, Long driverId) {
        confirmDays(List.of(applicationDayId), driverId);
    }

    // ── Confirm several days for ONE applicant in a single, race-safe action ─

    @Transactional
    public void confirmDays(List<Long> applicationDayIds, Long driverId) {

        List<Long> distinctIds = dedupeIds(applicationDayIds);
        List<ScheduledRideApplicationDay> days = loadAndValidateSameApplicant(distinctIds);

        ScheduledRideApplication application = days.get(0).getApplication();
        User booker = application.getBooker();

        List<Long> instanceIds = days.stream()
                .map(d -> d.getInstance().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, ScheduledRideInstance> lockedInstances = new LinkedHashMap<>();
        for (Long instanceId : instanceIds) {
            ScheduledRideInstance locked = instanceRepo.findByIdForUpdate(instanceId)
                    .orElseThrow(() -> AppException.notFound("Ride instance not found"));
            lockedInstances.put(instanceId, locked);
        }

        for (ScheduledRideApplicationDay day : days) {
            ScheduledRideTemplate template = day.getInstance().getTemplate();
            if (!template.getPostedBy().getId().equals(driverId)) {
                throw AppException.forbidden("Only the ride driver can confirm applicants.");
            }
            if (day.getStatus() != ApplicationStatus.PENDING) {
                throw AppException.conflict("Application for " + day.getInstance().getRideDate()
                        + " is no longer pending. Status: " + day.getStatus());
            }
            ScheduledRideInstance locked = lockedInstances.get(day.getInstance().getId());
            if (locked.getState() != RideState.OPEN) {
                throw AppException.conflict(locked.getRideDate()
                        + " is no longer open. State: " + locked.getState());
            }
        }

        String driverName = days.get(0).getInstance().getTemplate().getPostedBy().getFullName();
        List<LocalDate> confirmedDates = new ArrayList<>();

        Map<Long, CascadeGroup> cascadeByApplication = new LinkedHashMap<>();

        for (ScheduledRideApplicationDay day : days) {
            ScheduledRideInstance instance = lockedInstances.get(day.getInstance().getId());
            LocalDate date = instance.getRideDate();

            day.setStatus(ApplicationStatus.CONFIRMED);
            appDayRepo.save(day);

            instance.setState(RideState.BOOKED);
            instance.setBookedBy(booker);
            instance.setBookerOtp(String.format("%04d", new java.security.SecureRandom().nextInt(10_000)));
            instance.setBookedAt(LocalDateTime.now());
            instanceRepo.save(instance);
            confirmedDates.add(date);

            List<ScheduledRideApplicationDay> otherPending = appDayRepo.findPendingByInstanceId(instance.getId())
                    .stream().filter(d -> !d.getId().equals(day.getId())).toList();

            for (ScheduledRideApplicationDay other : otherPending) {
                other.setStatus(ApplicationStatus.REJECTED);
                other.setDeleted(true);
                other.setDeletedAt(LocalDateTime.now());
                appDayRepo.save(other);

                Long otherApplicationId = other.getApplication().getId();
                Long otherBookerId = other.getApplication().getBooker().getId();

                cascadeByApplication
                        .computeIfAbsent(otherApplicationId, id -> new CascadeGroup(otherBookerId))
                        .dates.add(date);

                log.info("Cascade-rejected other pending app for same day: appDayId={} bookerId={} date={}",
                        other.getId(), otherBookerId, date);
            }
        }

        eventPublisher.publishEvent(new ScheduledDaysConfirmedEvent(booker.getId(), driverName, confirmedDates));

        for (Map.Entry<Long, CascadeGroup> entry : cascadeByApplication.entrySet()) {
            Long otherApplicationId = entry.getKey();
            CascadeGroup group = entry.getValue();
            long remainingActive = appDayRepo.countByApplicationIdAndStatusIn(otherApplicationId, ACTIVE_STATUSES);
            eventPublisher.publishEvent(new ScheduledDaysUnavailableEvent(
                    group.bookerId, driverName, group.dates, remainingActive > 0));
        }

        log.info("Scheduled days confirmed: applicationId={} bookerId={} dates={}",
                application.getId(), booker.getId(), confirmedDates);
    }

    private static class CascadeGroup {
        final Long bookerId;
        final List<LocalDate> dates = new ArrayList<>();
        CascadeGroup(Long bookerId) { this.bookerId = bookerId; }
    }

    // ── Reject one day (Driver) ─────────────────────────────────────────────

    @Transactional
    public void rejectDay(Long applicationDayId, Long driverId) {
        rejectDays(List.of(applicationDayId), driverId);
    }

    // ── Reject several days for ONE applicant in a single action (Driver) ───

    @Transactional
    public void rejectDays(List<Long> applicationDayIds, Long driverId) {

        List<Long> distinctIds = dedupeIds(applicationDayIds);
        List<ScheduledRideApplicationDay> days = loadAndValidateSameApplicant(distinctIds);

        ScheduledRideApplication app = days.get(0).getApplication();
        User booker = app.getBooker();
        String driverName = days.get(0).getInstance().getTemplate().getPostedBy().getFullName();
        List<LocalDate> rejectedDates = new ArrayList<>();

        for (ScheduledRideApplicationDay day : days) {
            ScheduledRideTemplate template = day.getInstance().getTemplate();
            if (!template.getPostedBy().getId().equals(driverId)) {
                throw AppException.forbidden("Only the ride driver can reject applicants.");
            }
            if (day.getStatus() != ApplicationStatus.PENDING && day.getStatus() != ApplicationStatus.CONFIRMED) {
                throw AppException.conflict("Application for " + day.getInstance().getRideDate()
                        + " is not active. Status: " + day.getStatus());
            }

            ScheduledRideInstance inst = day.getInstance();
            if (day.getStatus() == ApplicationStatus.CONFIRMED) {
                inst.setState(RideState.OPEN);
                inst.setBookedBy(null);
                inst.setBookedAt(null);
                inst.setBookerOtp(null);
                instanceRepo.save(inst);
            }

            day.setStatus(ApplicationStatus.REJECTED);
            appDayRepo.save(day);
            rejectedDates.add(inst.getRideDate());
        }

        eventPublisher.publishEvent(new ScheduledDaysRejectedEvent(booker.getId(), driverName, rejectedDates));

        log.info("Scheduled days rejected: appDayIds={} by driverId={}", distinctIds, driverId);
    }

    // ── Withdraw one day (Booker) ───────────────────────────────────────────

    @Transactional
    public void withdrawDay(Long applicationDayId, Long bookerId) {
        withdrawDays(List.of(applicationDayId), bookerId);
    }

    // ── Withdraw several days for ONE application in a single action (Booker)

    @Transactional
    public void withdrawDays(List<Long> applicationDayIds, Long bookerId) {
        List<Long> distinctIds = dedupeIds(applicationDayIds);
        List<ScheduledRideApplicationDay> days = appDayRepo.findActiveByIdIn(distinctIds);
        if (days.size() != distinctIds.size()) {
            throw AppException.notFound("One or more applications not found.");
        }

        List<LocalDate> withdrawnDates = new ArrayList<>();
        Long driverId = null;
        String bookerName = null;

        for (ScheduledRideApplicationDay day : days) {
            if (!day.getApplication().getBooker().getId().equals(bookerId)) {
                throw AppException.forbidden("You can only withdraw your own applications.");
            }
            if (day.getStatus() != ApplicationStatus.PENDING && day.getStatus() != ApplicationStatus.CONFIRMED) {
                throw AppException.conflict("Application for " + day.getInstance().getRideDate()
                        + " cannot be withdrawn. Current status: " + day.getStatus());
            }

            ScheduledRideInstance inst = day.getInstance();
            driverId = inst.getTemplate().getPostedBy().getId();
            bookerName = day.getApplication().getBooker().getFullName();

            if (day.getStatus() == ApplicationStatus.CONFIRMED) {
                inst.setState(RideState.OPEN);
                inst.setBookedBy(null);
                inst.setBookedAt(null);
                inst.setBookerOtp(null);
                instanceRepo.save(inst);
            }

            day.setStatus(ApplicationStatus.WITHDRAWN);
            appDayRepo.save(day);
            withdrawnDates.add(inst.getRideDate());
        }

        if (driverId != null && bookerName != null) {
            fcmService.sendToUser(
                    driverId,
                    "Application withdrawn",
                    bookerName + " withdrew request for " + withdrawnDates + ".",
                    Map.of(
                            "type", "SCHEDULED_APPLICATION_WITHDRAWN",
                            "dates", withdrawnDates.toString()
                    )
            );
        }

        log.info("Scheduled days withdrawn by booker: appDayIds={} bookerId={}", distinctIds, bookerId);
    }

    private List<Long> dedupeIds(List<Long> applicationDayIds) {
        if (applicationDayIds == null || applicationDayIds.isEmpty()) {
            throw AppException.badRequest("No applications selected.");
        }
        return applicationDayIds.stream().distinct().toList();
    }

    private List<ScheduledRideApplicationDay> loadAndValidateSameApplicant(List<Long> applicationDayIds) {
        List<ScheduledRideApplicationDay> days = appDayRepo.findActiveByIdIn(applicationDayIds);
        if (days.size() != applicationDayIds.size()) {
            throw AppException.notFound("One or more applications not found.");
        }

        Long applicationId = days.get(0).getApplication().getId();
        boolean sameApplication = days.stream()
                .allMatch(d -> d.getApplication().getId().equals(applicationId));
        if (!sameApplication) {
            throw AppException.badRequest("All selected days must belong to the same applicant.");
        }

        return days;
    }

    // ── Driver: list applicants for a template, grouped by applicant ────────

    @Transactional(readOnly = true)
    public List<ScheduledRideApplicantResponse> listApplicants(Long templateId, Long driverId) {
        ScheduledRideTemplate template = templateRepo.findByIdWithDetails(templateId)
                .orElseThrow(() -> AppException.notFound("Scheduled ride not found"));
        if (!template.getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("Only the ride driver can view applicants.");
        }

        List<ScheduledRideApplicationDay> allDays = appDayRepo.findActiveByTemplateId(templateId);

        Map<Long, List<ScheduledRideApplicationDay>> byApplication = allDays.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getApplication().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return byApplication.values().stream()
                .map(group -> ScheduledRideApplicantResponse.from(group.get(0).getApplication(), group))
                .toList();
    }

    // ── Instance-level ride flow: start / verify / complete ──────────────────

    @Transactional
    public void startInstance(Long instanceId, Long driverId) {
        ScheduledRideInstance inst = instanceRepo.findByIdWithDetails(instanceId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        if (!inst.getTemplate().getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("Only the driver can start the ride.");
        }
        if (inst.getState() != RideState.BOOKED) {
            throw AppException.conflict("Ride cannot be started. Current state: " + inst.getState());
        }

        inst.setState(RideState.STARTED);
        inst.setStartedAt(LocalDateTime.now());
        instanceRepo.save(inst);

        if (inst.getBookedBy() != null) {
            fcmService.notifyBookerScheduledRideStarted(
                    inst.getBookedBy().getId(),
                    inst.getTemplate().getPostedBy().getFullName(),
                    inst.getId());
        }
        log.info("Scheduled ride instance started: instanceId={}", instanceId);
    }

    @Transactional
    public void verifyInstanceOtp(Long instanceId, String submittedOtp, Long driverId) {
        ScheduledRideInstance inst = instanceRepo.findByIdWithDetails(instanceId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        if (!inst.getTemplate().getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("Only the driver can verify the OTP.");
        }
        if (inst.getState() != RideState.STARTED) {
            throw AppException.conflict("OTP can only be verified in STARTED state. Current: " + inst.getState());
        }
        if (inst.getBookerOtp() == null || !inst.getBookerOtp().equals(submittedOtp)) {
            throw AppException.badRequest("Invalid OTP.");
        }

        inst.setState(RideState.VERIFIED);
        inst.setVerifiedAt(LocalDateTime.now());
        instanceRepo.save(inst);

        log.info("Scheduled ride instance OTP verified: instanceId={}", instanceId);
    }

    @Transactional
    public void completeInstance(Long instanceId, Long driverId) {
        ScheduledRideInstance inst = instanceRepo.findByIdWithDetails(instanceId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        if (!inst.getTemplate().getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("Only the driver can complete the ride.");
        }
        boolean isStarted  = inst.getState() == RideState.STARTED;
        boolean isVerified = inst.getState() == RideState.VERIFIED;
        if (!isStarted && !isVerified) {
            throw AppException.conflict(
                    "Ride must be STARTED or VERIFIED to complete. Current: " + inst.getState());
        }
        if (isStarted && inst.getVerifiedAt() == null) {
            throw AppException.conflict("Booker has not verified the OTP yet.");
        }

        inst.setState(RideState.COMPLETED);
        inst.setCompletedAt(LocalDateTime.now());
        instanceRepo.save(inst);

        if (inst.getBookedBy() != null) {
            fcmService.notifyBookerScheduledRideCompleted(
                    inst.getBookedBy().getId(),
                    inst.getTemplate().getPostedBy().getFullName(),
                    inst.getId());
        }
        log.info("Scheduled ride instance completed: instanceId={}", instanceId);
    }
}
