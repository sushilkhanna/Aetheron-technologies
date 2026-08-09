package com.bikepooling.service;

import com.bikepooling.config.OsrmClient;
import com.bikepooling.dto.request.ApplyRideRequest;
import com.bikepooling.dto.response.RideApplicantSummaryResponse;
import com.bikepooling.dto.response.RideApplicationResponse;
import com.bikepooling.entity.*;
import com.bikepooling.enums.ApplicationStatus;
import com.bikepooling.enums.RideState;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.*;
import com.bikepooling.util.FareUtil;
import com.bikepooling.util.RouteMatchUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static com.bikepooling.enums.ApplicationStatus.REJECTED;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideApplicationService {

    private final RideApplicationRepository applicationRepo;
    private final RideStatusRepository      rideStatusRepo;
    private final UserRepository            userRepo;
    private final FcmService                fcmService;
    private final OsrmClient                osrmClient;
    private final AppConfigService          configService;
    private final RideApplicationWriter     applicationWriter;
    private final RideService               rideService;
    private final LiveRideService           liveRideService;

    private static final List<ApplicationStatus> ACTIVE_APPLICATION_STATUSES = List.of(
            ApplicationStatus.PENDING, ApplicationStatus.CONFIRMED
    );

    // ── apply ──────────────────────────────────────────────────────────────────

    public RideApplicationResponse apply(Long rideId, ApplyRideRequest req, Long bookerId) {

        RideStatus status = getRideStatusOrThrow(rideId);
        Ride ride = status.getRide();

        if (status.getState() != RideState.OPEN) {
            throw AppException.conflict(
                    "This ride is not accepting applications. State: " + status.getState());
        }
        if (ride.getPostedBy().getId().equals(bookerId)) {
            throw AppException.badRequest("You cannot apply to your own ride.");
        }

        LocalDateTime applicationCutoff = ride.getDepartAt().plusMinutes(15);
        if (LocalDateTime.now().isAfter(applicationCutoff)) {
            throw AppException.conflict("This ride is no longer accepting applications.");
        }
        if (req.getPickupLat().compareTo(req.getDropLat()) == 0
                && req.getPickupLng().compareTo(req.getDropLng()) == 0) {
            throw AppException.badRequest("Pickup and drop location cannot be the same.");
        }
        if (applicationRepo.existsByRideAndBooker(rideId, bookerId, ACTIVE_APPLICATION_STATUSES)) {
            throw AppException.conflict("You have already applied to this ride.");
        }
        // NOTE: reverted to the repository's actual method name — hasFinishBooking
        // does not exist on RideApplicationRepository and would not compile.
        if (applicationRepo.hasFinishBooking(bookerId)) {
            throw AppException.conflict(
                    "You already have a confirmed booking. Cancel it before applying elsewhere.");
        }

        User booker = userRepo.findById(bookerId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        RouteMatchUtil.MatchResult stage1 = RouteMatchUtil.evaluateStage1(
                ride.getFromLat().doubleValue(), ride.getFromLng().doubleValue(),
                ride.getToLat().doubleValue(),   ride.getToLng().doubleValue(),
                ride.getDistanceKm().doubleValue(),
                req.getPickupLat().doubleValue(), req.getPickupLng().doubleValue(),
                req.getDropLat().doubleValue(),   req.getDropLng().doubleValue()
        );
        if (!stage1.isMatched()) {
            throw AppException.badRequest(stage1.getReason());
        }

        OsrmClient.RouteLegs legs = osrmClient.getRouteLegs(
                new double[]{ride.getFromLat().doubleValue(), req.getPickupLat().doubleValue(),
                        req.getDropLat().doubleValue(),  ride.getToLat().doubleValue()},
                new double[]{ride.getFromLng().doubleValue(), req.getPickupLng().doubleValue(),
                        req.getDropLng().doubleValue(),  ride.getToLng().doubleValue()}
        );

        double detourKm = Math.max(legs.getTotalKm() - ride.getDistanceKm().doubleValue(), 0.0);
        RouteMatchUtil.MatchResult stage2 = RouteMatchUtil.checkDetourBudget(
                detourKm, ride.getExtraDistanceKm().doubleValue());
        if (!stage2.isMatched()) {
            throw AppException.badRequest(stage2.getReason());
        }

        double bookerRoadKm = legs.getLeg(1);
        BigDecimal bookerDistanceKm = BigDecimal.valueOf(bookerRoadKm)
                .setScale(2, RoundingMode.HALF_UP);

        // Single source of truth for the fare formula — see FareUtil.
        BigDecimal bookerFare = FareUtil.calculateBookerFare(
                bookerRoadKm, ride.getDistanceKm().doubleValue(),
                ride.getFare(), configService.getMinFare());

        RideApplicationResponse response =
                applicationWriter.save(rideId, booker, req, bookerDistanceKm, bookerFare);

        try {
            fcmService.notifyDriverNewApplicant(
                    ride.getPostedBy().getId(), booker.getFullName(), rideId);
        } catch (Exception e) {
            log.warn("Failed to notify driver of new applicant: rideId={} err={}",
                    rideId, e.getMessage());
        }

        return response;
    }

    // ── withdraw ───────────────────────────────────────────────────────────────

    @Transactional
    public void withdraw(Long applicationId, Long bookerId) {

        RideApplication application = getActiveApplicationOrThrow(applicationId);

        if (!application.getBooker().getId().equals(bookerId)) {
            throw AppException.forbidden("You can only withdraw your own application.");
        }
        if (application.getStatus() == ApplicationStatus.CONFIRMED) {
            throw AppException.badRequest(
                    "Confirmed rides cannot be withdrawn. Please cancel the ride instead.");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw AppException.badRequest(
                    "Application cannot be withdrawn in its current state: "
                            + application.getStatus());
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setDeleted(true);
        application.setDeletedAt(LocalDateTime.now());
        applicationRepo.save(application);

        log.info("Application withdrawn: id={} by bookerId={}", applicationId, bookerId);
    }

    // ── confirm ────────────────────────────────────────────────────────────────

    @Transactional
    public void confirm(Long applicationId, Long driverId) {
        try {
            RideApplication application = getActiveApplicationOrThrow(applicationId);

            if (!application.getRide().getPostedBy().getId().equals(driverId)) {
                throw AppException.forbidden("Only the ride driver can confirm applicants.");
            }
            if (application.getStatus() != ApplicationStatus.PENDING) {
                throw AppException.conflict(
                        "Application is no longer pending. Status: " + application.getStatus());
            }

            Long confirmedRideId = application.getRide().getId();

            RideStatus rideStatus = getRideStatusOrThrow(confirmedRideId);

            if (rideStatus.getState() != RideState.OPEN
                    && rideStatus.getState() != RideState.LIVE) {
                throw AppException.conflict(
                        "Ride is no longer open. State: " + rideStatus.getState());
            }

            // Fare is NOT recalculated here — it was already locked in at apply()
            // time via FareUtil and stored on the application. Confirming a booker
            // just moves the ride state forward; it never touches bookerFare.
            boolean wasLive  = rideStatus.getState() == RideState.LIVE;
            String driverName = application.getRide().getPostedBy().getFullName();

            application.setStatus(ApplicationStatus.CONFIRMED);
            applicationRepo.save(application);

            rideService.generateBookingOtp(confirmedRideId);

            User booker = application.getBooker();

            if (wasLive) {
                rideStatus.setState(RideState.STARTED);
                rideStatus.setStartedAt(LocalDateTime.now());
            } else {
                rideStatus.setState(RideState.BOOKED);
                rideStatus.setBookedAt(LocalDateTime.now());
            }

            rideStatus.setBookedBy(booker);
            rideStatusRepo.save(rideStatus);

            if (wasLive) {
                liveRideService.onRideBooked(
                        confirmedRideId,
                        application.getPickupLat().doubleValue(),
                        application.getPickupLng().doubleValue(),
                        application.getDropLat().doubleValue(),
                        application.getDropLng().doubleValue()
                );
                log.info("Live ride confirmed → STARTED: rideId={} bookerId={}",
                        confirmedRideId, booker.getId());
            }

            List<Long> rejectedBookerIds = applicationRepo.findPendingBookerIds(
                    applicationId, confirmedRideId);

            applicationRepo.rejectOtherApplicants(
                    confirmedRideId, applicationId, LocalDateTime.now());

            for (Long rejectedBookerId : rejectedBookerIds) {
                try {
                    fcmService.notifyBookerApplicationRejected(
                            rejectedBookerId, driverName, confirmedRideId);
                } catch (Exception e) {
                    log.warn("Rejected-applicant notification failed: bookerId={} rideId={} err={}",
                            rejectedBookerId, confirmedRideId, e.getMessage());
                }
            }

            int withdrawnCount = applicationRepo.withdrawOtherPendingApplications(
                    booker.getId(), confirmedRideId, LocalDateTime.now());

            fcmService.notifyBookerApplicationConfirmed(
                    booker.getId(), driverName, confirmedRideId);

            log.info("Application confirmed: id={} rideId={} bookerId={} wasLive={} " +
                            "newState={} | {} others rejected | {} other applications withdrawn",
                    applicationId, confirmedRideId, booker.getId(), wasLive,
                    rideStatus.getState(), rejectedBookerIds.size(), withdrawnCount);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw AppException.conflict(
                    "This ride was just booked by another request. Please refresh.");
        }
    }

    // ── reject ─────────────────────────────────────────────────────────────────

    @Transactional
    public void reject(Long applicationId, Long driverId) {

        RideApplication application = getActiveApplicationOrThrow(applicationId);

        if (!application.getRide().getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("Only the ride driver can reject applicants.");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw AppException.conflict(
                    "Application is no longer pending. Status: " + application.getStatus());
        }

        application.setStatus(REJECTED);
        applicationRepo.save(application);

        fcmService.notifyBookerApplicationRejected(
                application.getBooker().getId(),
                application.getRide().getPostedBy().getFullName(),
                application.getRide().getId()
        );

        log.info("Application rejected: id={} by driverId={}", applicationId, driverId);
    }

    // ── list pending applicants (driver view — trimmed, no exact coordinates) ──

    @Transactional(readOnly = true)
    public List<RideApplicantSummaryResponse> listApplicants(Long rideId, Long driverId) {

        RideStatus status = getRideStatusOrThrow(rideId);

        if (!status.getRide().getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("Only the ride driver can view applicants.");
        }
        if (status.getState() != RideState.OPEN && status.getState() != RideState.LIVE) {
            throw AppException.conflict("Ride is no longer open. State: " + status.getState());
        }

        return applicationRepo.findPendingByRideId(rideId)
                .stream()
                .map(RideApplicantSummaryResponse::from)
                .toList();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private RideApplication getActiveApplicationOrThrow(Long applicationId) {
        return applicationRepo.findActiveById(applicationId)
                .orElseThrow(() -> AppException.notFound(
                        "Application not found: " + applicationId));
    }

    private RideStatus getRideStatusOrThrow(Long rideId) {
        return rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found: " + rideId));
    }
}