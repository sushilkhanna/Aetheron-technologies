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

        Set<LocalDate> dates = req.getDates();
        validateDates(dates);

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

        LocalDate weekStart = dates.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate weekEnd   = dates.stream().max(LocalDate::compareTo).orElse(LocalDate.now());

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
                .dates(dates)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .status(ScheduledRideStatus.ACTIVE)
                .deleted(false)
                .build();

        template = templateRepo.save(template);
        generateInstancesForDates(template, dates);

        log.info("Scheduled ride posted: templateId={} driverId={} dates={}",
                template.getId(), userId, dates);

        return ScheduledRideTemplateResponse.from(template);
    }

    // ── Update ──

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

        if (req.getDates() != null && !req.getDates().isEmpty()) {
            validateDates(req.getDates());

            Set<LocalDate> oldDates = new HashSet<>(template.getDates());
            Set<LocalDate> newDates = req.getDates();

            Set<LocalDate> removed = new HashSet<>(oldDates);
            removed.removeAll(newDates);
            Set<LocalDate> added = new HashSet<>(newDates);
            added.removeAll(oldDates);

            // Guard: Only OPEN instances can be removed or modified!
            if (!removed.isEmpty()) {
                List<ScheduledRideInstance> removedInstances = instanceRepo.findByTemplateIdAndRideDateIn(templateId, removed);
                for (ScheduledRideInstance inst : removedInstances) {
                    if (inst.getState() != RideState.OPEN) {
                        throw AppException.conflict("Date " + inst.getRideDate()
                                + " cannot be removed or modified because its ride status is " + inst.getState() + ".");
                    }
                }

                instanceRepo.cancelOpenInstancesForDates(templateId, removed, LocalDateTime.now());
                log.info("Removed OPEN dates cancelled: templateId={} dates={}", templateId, removed);
            }

            template.setDates(newDates);
            template.setWeekStart(newDates.stream().min(LocalDate::compareTo).orElse(LocalDate.now()));
            template.setWeekEnd(newDates.stream().max(LocalDate::compareTo).orElse(LocalDate.now()));
            templateRepo.save(template);

            if (!added.isEmpty()) {
                generateInstancesForDates(template, added);
                log.info("New dates generated instances: templateId={} dates={}", templateId, added);
            }
        }

        if (req.getExtraDistanceKm() != null) {
            template.setExtraDistanceKm(req.getExtraDistanceKm());
            templateRepo.save(template);
            instanceRepo.refreshOpenInstances(templateId, template.getDepartTime(), req.getExtraDistanceKm());
            log.info("Template extraKm updated, propagated to OPEN instances: templateId={}", templateId);
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

    private void generateInstancesForDates(ScheduledRideTemplate template, Set<LocalDate> dates) {
        for (LocalDate date : dates) {
            boolean exists = instanceRepo.findByTemplateIdAndRideDate(
                    template.getId(), date).isPresent();
            if (!exists) {
                instanceRepo.save(ScheduledRideInstance.builder()
                        .template(template)
                        .rideDate(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .departTime(template.getDepartTime())
                        .extraDistanceKm(template.getExtraDistanceKm())
                        .state(RideState.OPEN)
                        .build());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateDates(Set<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            throw AppException.badRequest("Select at least one date.");
        }
        LocalDate today = LocalDate.now();
        LocalDate maxAllowedDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(7);

        for (LocalDate date : dates) {
            if (date.isBefore(today)) {
                throw AppException.badRequest("Date " + date + " cannot be in the past.");
            }
            if (date.isAfter(maxAllowedDate)) {
                throw AppException.badRequest("Date " + date + " is beyond next week (" + maxAllowedDate
                        + "). Drivers can only post for current and next week.");
            }
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