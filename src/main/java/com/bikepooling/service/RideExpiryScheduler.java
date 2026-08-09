package com.bikepooling.service;

import com.bikepooling.enums.RideState;
import com.bikepooling.repository.RideAlertRepository;
import com.bikepooling.repository.RideApplicationRepository;
import com.bikepooling.repository.RideRequestRepository;
import com.bikepooling.repository.RideStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideExpiryScheduler {

    private final RideStatusRepository      rideStatusRepo;
    private final RideApplicationRepository applicationRepo;
    private final RideAlertRepository       alertRepo;
    private final RideRequestRepository     rideRequestRepo;
    private final FcmService                fcmService;

    private static final long EXPIRY_GRACE_MINUTES = 15;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        sendPreDepartureNotifications(now);
        expireStaleOpenRides(now);
        cleanupExpiredAlerts(now);
        cleanupExpiredRequests(now);
    }

    private void sendPreDepartureNotifications(LocalDateTime now) {
        LocalDateTime windowStart = now.plusMinutes(10);
        LocalDateTime windowEnd   = now.plusMinutes(20);

        rideStatusRepo
                .findRidesInWindowWithStateNotYetNotified(windowStart, windowEnd, RideState.OPEN)
                .forEach(rs -> {
                    log.info("Pre-expiry warning: rideId={}", rs.getRide().getId());
                    fcmService.sendToUser(
                            rs.getRide().getPostedBy().getId(),
                            "Your ride is about to expire",
                            "No one has booked your ride yet. It will expire "
                                    + EXPIRY_GRACE_MINUTES + " minutes after departure if no booking.",
                            Map.of("type",   "RIDE_EXPIRY_WARNING",
                                    "rideId", String.valueOf(rs.getRide().getId()))
                    );
                    rs.setPreDepartureNotifiedAt(now);
                    rideStatusRepo.save(rs);
                });

        rideStatusRepo
                .findRidesInWindowWithStateNotYetNotified(windowStart, windowEnd, RideState.BOOKED)
                .forEach(rs -> {
                    log.info("Get-ready notification: rideId={}", rs.getRide().getId());

                    fcmService.sendToUser(
                            rs.getRide().getPostedBy().getId(),
                            "Get ready for your ride!",
                            "Your ride departs in ~15 minutes. Your passenger is waiting.",
                            Map.of("type",   "RIDE_STARTING_SOON",
                                    "rideId", String.valueOf(rs.getRide().getId()))
                    );

                    if (rs.getBookedBy() != null) {
                        fcmService.sendToUser(
                                rs.getBookedBy().getId(),
                                "Your ride is starting soon!",
                                "Your ride departs in ~15 minutes. Head to your pickup point.",
                                Map.of("type",   "RIDE_STARTING_SOON",
                                        "rideId", String.valueOf(rs.getRide().getId()))
                        );
                    }

                    rs.setPreDepartureNotifiedAt(now);
                    rideStatusRepo.save(rs);
                });
    }

    /**
     * Only OPEN rides are expired — BOOKED/STARTED are never touched here.
     * Grace period: 15 minutes past departAt.
     */
    private void expireStaleOpenRides(LocalDateTime now) {
        LocalDateTime expiryThreshold = now.minusMinutes(EXPIRY_GRACE_MINUTES);

        List<Long> expiredRideIds = rideStatusRepo.findExpiredOpenRideIds(expiryThreshold);
        if (expiredRideIds.isEmpty()) return;

        log.info("Expiring {} stale OPEN rides: {}", expiredRideIds.size(), expiredRideIds);
        rideStatusRepo.markRidesExpired(expiredRideIds, RideState.EXPIRED);

        expiredRideIds.forEach(rideId ->
                applicationRepo.expireApplicationsForRide(rideId, now));
    }

    private void cleanupExpiredAlerts(LocalDateTime now) {
        int deactivated = alertRepo.deactivateExpiredAlerts(now);
        if (deactivated > 0) {
            log.info("Deactivated {} expired alerts.", deactivated);
        }
    }

    private void cleanupExpiredRequests(LocalDateTime now) {
        int deactivated = rideRequestRepo.deactivateExpiredRequests(now);
        if (deactivated > 0) {
            log.info("Deactivated {} expired ride requests.", deactivated);
        }
    }
}