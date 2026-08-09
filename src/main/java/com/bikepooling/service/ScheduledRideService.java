package com.bikepooling.service;

import com.bikepooling.config.OsrmClient;
import com.bikepooling.dto.request.PostScheduledRideRequest;
import com.bikepooling.dto.request.UpdateScheduledRideRequest;
import com.bikepooling.dto.response.ScheduledRideInstanceResponse;
import com.bikepooling.dto.response.ScheduledRideTemplateResponse;
import com.bikepooling.entity.*;
import com.bikepooling.enums.*;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledRideService {

    private static final int MAX_ACTIVE_TEMPLATES = 2;
    private static final double MIN_RIDE_DISTANCE_KM = 0.2;

    private static final List<RideState> CANCELLABLE_STATES =
            List.of(RideState.OPEN, RideState.BOOKED, RideState.STARTED);

    private final ScheduledRideTemplateRepository templateRepo;
    private final ScheduledRideInstanceRepository instanceRepo;
    private final ScheduledRideApplicationDayRepository appDayRepo;
    private final UserRepository userRepo;
    private final VehicleRepository vehicleRepo;
    private final AppConfigService configService;
    private final OsrmClient osrmClient;
    private final FcmService fcmService;

    // ── Post ──────────────────────────────────────────────────────────────────

    @Transactional
    public ScheduledRideTemplateResponse postScheduledRide(PostScheduledRideRequest req, Long userId) {

        User driver = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        if (!driver.isDlVerified()) {
            throw AppException.forbidden("Driving licence verification is required to post a ride.");
        }

        LocalDate today        = LocalDate.now();
        LocalDate thisWeekEnd  = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        LocalDate weekStart;
        LocalDate weekEnd;
        if (req.getWeek() == ScheduleWeek.CURRENT) {
            // Today → Saturday this week. If today IS Saturday, this is just today — single day.
            weekStart = today;
            weekEnd   = thisWeekEnd;
        } else {
            // Full clean 7-day block: Sunday after this week's Saturday → the Saturday after that.
            // Entirely in the future regardless of what day "today" is — this is what lets a driver
            // posting on a Saturday still get a full week to choose from, without mixing weeks.
            weekStart = thisWeekEnd.plusDays(1);
            weekEnd   = weekStart.plusDays(6);
        }

        validateDays(req.getDays());

        double roadKm = osrmClient.getRoadDistanceKm(
                req.getFromLat().doubleValue(), req.getFromLng().doubleValue(),
                req.getToLat().doubleValue(),   req.getToLng().doubleValue());
        if (roadKm < MIN_RIDE_DISTANCE_KM) {
            throw AppException.badRequest(
                    "Source and destination are too close. Minimum ride distance is 200 metres.");
        }

        Vehicle vehicle = vehicleRepo.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> AppException.notFound("Please add a vehicle to your profile first."));
        if (!vehicle.isActive()) {
            throw AppException.badRequest("Your vehicle is currently inactive.");
        }

        long activeCount = templateRepo.countActiveByDriver(userId);
        if (activeCount >= MAX_ACTIVE_TEMPLATES) {
            throw AppException.conflict(
                    "You already have " + MAX_ACTIVE_TEMPLATES
                            + " active scheduled rides. Cancel one before posting more.");
        }

        BigDecimal distanceKm = BigDecimal.valueOf(roadKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fare       = calculateFare(distanceKm);
        PreferredGender gender = resolveGenderPreference(req.getPreferredGender(), driver);

        ScheduledRideTemplate template = ScheduledRideTemplate.builder()
                .postedBy(driver)
                .vehicle(vehicle)
                .fromName(req.getFromName())
                .fromLat(req.getFromLat())
                .fromLng(req.getFromLng())
                .toName(req.getToName())
                .toLat(req.getToLat())
                .toLng(req.getToLng())
                .departTime(req.getDepartTime())
                .distanceKm(distanceKm)
                .fare(fare)
                .extraDistanceKm(req.getExtraDistanceKm())
                .paymentMode(req.getPaymentMode())
                .preferredGender(gender)
                .routeNotes(req.getRouteNotes())
                .days(req.getDays())
                .scheduledWeek(req.getWeek())
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .status(ScheduledRideStatus.ACTIVE)
                .deleted(false)
                .build();

        template = templateRepo.save(template);
        generateInstances(template, weekStart, weekEnd, req.getDays());

        log.info("Scheduled ride posted: templateId={} driverId={} week={} range=[{},{}] days={}",
                template.getId(), userId, req.getWeek(), weekStart, weekEnd, req.getDays());

        return ScheduledRideTemplateResponse.from(template);
    }

    // ── Update (days / time / extraKm — never location, never the week block) ──

    @Transactional
    public ScheduledRideTemplateResponse updateScheduledRide(
            Long templateId, UpdateScheduledRideRequest req, Long userId) {

        ScheduledRideTemplate template = templateRepo.findByIdWithDetails(templateId)
                .orElseThrow(() -> AppException.notFound("Scheduled ride not found"));

        if (!template.getPostedBy().getId().equals(userId)) {
            throw AppException.forbidden("You can only edit your own scheduled ride.");
        }
        if (template.getStatus() != ScheduledRideStatus.ACTIVE) {
            throw AppException.conflict("This scheduled ride has been cancelled.");
        }

        LocalDate today = LocalDate.now();
        // Never generate instances for dates already passed within the template's own window.
        LocalDate effectiveStart = today.isAfter(template.getWeekStart())
                ? today : template.getWeekStart();

        if (req.getDays() != null && !req.getDays().isEmpty()) {
            validateDays(req.getDays());

            Set<DayOfWeek> oldDays = new HashSet<>(template.getDays());
            Set<DayOfWeek> newDays = req.getDays();

            Set<DayOfWeek> removed = new HashSet<>(oldDays);
            removed.removeAll(newDays);
            Set<DayOfWeek> added = new HashSet<>(newDays);
            added.removeAll(oldDays);

            if (!removed.isEmpty()) {
                instanceRepo.cancelOpenInstancesForDays(templateId, removed, LocalDateTime.now());
                log.info("Removed days cancelled OPEN instances: templateId={} days={}", templateId, removed);
            }

            template.setDays(newDays);
            templateRepo.save(template);

            if (!added.isEmpty()) {
                generateInstances(template, effectiveStart, template.getWeekEnd(), added);
                log.info("New days generated instances: templateId={} days={}", templateId, added);
            }
        }

        boolean timeChanged  = req.getDepartTime() != null;
        boolean extraChanged = req.getExtraDistanceKm() != null;

        if (timeChanged || extraChanged) {
            LocalTime newTime   = timeChanged  ? req.getDepartTime()      : template.getDepartTime();
            BigDecimal newExtra = extraChanged ? req.getExtraDistanceKm() : template.getExtraDistanceKm();

            template.setDepartTime(newTime);
            template.setExtraDistanceKm(newExtra);
            templateRepo.save(template);

            instanceRepo.refreshOpenInstances(templateId, newTime, newExtra);
            log.info("Template time/extraKm updated, propagated to OPEN instances only: templateId={}",
                    templateId);
        }

        return ScheduledRideTemplateResponse.from(template);
    }

    // ── Cancel whole scheduled ride ─────────────────────────────────────────────

    @Transactional
    public void cancelScheduledRide(Long templateId, Long userId) {
        ScheduledRideTemplate template = templateRepo.findByIdWithDetails(templateId)
                .orElseThrow(() -> AppException.notFound("Scheduled ride not found"));

        if (!template.getPostedBy().getId().equals(userId)) {
            throw AppException.forbidden("You can only cancel your own scheduled ride.");
        }
        if (template.getStatus() == ScheduledRideStatus.CANCELLED) {
            throw AppException.conflict("This scheduled ride is already cancelled.");
        }

        template.setStatus(ScheduledRideStatus.CANCELLED);
        templateRepo.save(template);

        List<ScheduledRideInstance> cancellable =
                instanceRepo.findByTemplateIdAndStateIn(templateId, CANCELLABLE_STATES);

        for (ScheduledRideInstance inst : cancellable) {
            cancelInstanceInternal(inst);
        }

        log.info("Scheduled ride cancelled: templateId={} instancesCancelled={}",
                templateId, cancellable.size());
    }

    // ── Cancel a single day ──────────────────────────────────────────────────────

    @Transactional
    public void cancelScheduledRideDay(Long templateId, LocalDate date, Long userId) {
        ScheduledRideTemplate template = templateRepo.findByIdWithDetails(templateId)
                .orElseThrow(() -> AppException.notFound("Scheduled ride not found"));

        if (!template.getPostedBy().getId().equals(userId)) {
            throw AppException.forbidden("You can only cancel your own scheduled ride.");
        }

        ScheduledRideInstance inst = instanceRepo.findByTemplateIdAndRideDate(templateId, date)
                .orElseThrow(() -> AppException.notFound("No ride found for that date."));

        if (!CANCELLABLE_STATES.contains(inst.getState())) {
            throw AppException.conflict(
                    "This day cannot be cancelled. Current state: " + inst.getState());
        }

        cancelInstanceInternal(inst);
        log.info("Single day cancelled: templateId={} date={}", templateId, date);
    }

    private void cancelInstanceInternal(ScheduledRideInstance inst) {
        User previousBooker = inst.getBookedBy();
        String driverName   = inst.getTemplate().getPostedBy().getFullName();
        LocalDate date       = inst.getRideDate();

        inst.setState(RideState.CANCELLED);
        inst.setCancelledAt(LocalDateTime.now());
        instanceRepo.save(inst);

        List<ScheduledRideApplicationDay> pending = appDayRepo.findPendingByInstanceId(inst.getId());
        for (ScheduledRideApplicationDay d : pending) {
            d.setStatus(ApplicationStatus.EXPIRED);
            d.setDeleted(true);
            d.setDeletedAt(LocalDateTime.now());
            appDayRepo.save(d);
            fcmService.notifyBookerScheduledDayUnavailable(
                    d.getApplication().getBooker().getId(), driverName, date, true);
        }

        if (previousBooker != null) {
            fcmService.notifyBookerScheduledRideCancelled(previousBooker.getId(), driverName, date);
        }
    }

    // ── Instance generation ──────────────────────────────────────────────────────

    private void generateInstances(ScheduledRideTemplate template,
                                   LocalDate from, LocalDate to,
                                   Set<DayOfWeek> daysToGenerate) {
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            if (daysToGenerate.contains(cursor.getDayOfWeek())) {
                boolean exists = instanceRepo.findByTemplateIdAndRideDate(
                        template.getId(), cursor).isPresent();
                if (!exists) {
                    instanceRepo.save(ScheduledRideInstance.builder()
                            .template(template)
                            .rideDate(cursor)
                            .dayOfWeek(cursor.getDayOfWeek())
                            .departTime(template.getDepartTime())
                            .extraDistanceKm(template.getExtraDistanceKm())
                            .state(RideState.OPEN)
                            .build());
                }
            }
            cursor = cursor.plusDays(1);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateDays(Set<DayOfWeek> days) {
        if (days.isEmpty()) {
            throw AppException.badRequest("Select at least one day.");
        }
    }

    private PreferredGender resolveGenderPreference(PreferredGender requested, User driver) {
        if (requested == null || requested == PreferredGender.ANY) return PreferredGender.ANY;
        if (driver.getGender() == Gender.FEMALE) return requested;
        throw AppException.badRequest("Gender preference can only be set by female drivers.");
    }

    private BigDecimal calculateFare(BigDecimal distanceKm) {
        BigDecimal farePerKm = configService.getFarePerKm();
        BigDecimal minFare   = configService.getMinFare();
        BigDecimal fare      = distanceKm.multiply(farePerKm).setScale(2, RoundingMode.HALF_UP);
        return fare.compareTo(minFare) < 0 ? minFare : fare;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ScheduledRideTemplateResponse> getMyScheduledRides(Long driverId) {
        return templateRepo.findByDriverAndStatus(driverId, ScheduledRideStatus.ACTIVE).stream()
                .map(ScheduledRideTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledRideInstanceResponse> getInstancesForTemplate(Long templateId, Long driverId) {
        ScheduledRideTemplate template = templateRepo.findByIdWithDetails(templateId)
                .orElseThrow(() -> AppException.notFound("Scheduled ride not found"));
        if (!template.getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("You can only view your own scheduled ride.");
        }
        return instanceRepo.findByTemplateId(templateId).stream()
                .map(ScheduledRideInstanceResponse::from)
                .toList();
    }
}