package com.bikepooling.service;

import com.bikepooling.dto.response.LiveRideLocationBroadcast;
import com.bikepooling.entity.LiveRide;
import com.bikepooling.enums.LiveRideState;
import com.bikepooling.repository.LiveRideRepository;
import com.bikepooling.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRideTrackingService {

    private final LiveRideCacheService cacheService;
    private final ScheduledRideLocationCacheService locationCacheService;
    private final RedisLocationCacheService redisLocationCacheService;
    private final LiveRideRepository liveRideRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmService fcmService;

    private static final String LOCATION_TOPIC = "/topic/live-rides/%d/location";
    private static final double NEAR_DROP_RADIUS_KM = 1.0;            // 1km drop radius
    private static final long   NEAR_DROP_TIME_MS   = 5 * 60 * 1000L;  // 5 minutes
    private static final double OFF_ROUTE_THRESHOLD_KM = 2.0;          // 2km off route
    private static final long   OFF_ROUTE_AUTO_COMPLETE_MS = 20 * 60 * 1000L; // 20 minutes

    /**
     * Process GPS location updates from a driver in live mode via WebSocket STOMP.
     */
    public void processLiveDriverLocation(Long liveRideId, double lat, double lng, Double bearing, Double speed, long timestamp, Long driverId) {
        // 1. Fetch current live ride state
        String stateStr = "CONFIRMED";
        Optional<LiveRide> rideOpt = liveRideRepo.findById(liveRideId);
        if (rideOpt.isPresent()) {
            stateStr = rideOpt.get().getState().name();
        }

        // 2. Save location ping into Redis Cache (key: live:live_ride:{liveRideId})
        redisLocationCacheService.saveLiveRideLocation(liveRideId, driverId, lat, lng, bearing, speed, timestamp, stateStr);

        // 3. Update driver's live session cache
        cacheService.updateDriverLocation(driverId, lat, lng, bearing, speed, timestamp);

        // 4. Update general location cache for safety monitoring
        locationCacheService.updateLocation(liveRideId, lat, lng, bearing, speed, timestamp, driverId);

        // 5. Evaluate state & safety rules
        evaluateLiveRideLocation(liveRideId, lat, lng, bearing, speed, timestamp);
    }

    @Transactional
    public void evaluateLiveRideLocation(Long liveRideId, double driverLat, double driverLng, Double bearing, Double speed, long timestamp) {
        Optional<LiveRide> rideOpt = liveRideRepo.findByIdWithDetails(liveRideId);
        if (rideOpt.isEmpty()) {
            redisLocationCacheService.removeLiveRideLocation(liveRideId);
            return;
        }

        LiveRide ride = rideOpt.get();
        LiveRideState state = ride.getState();

        // ── STOMP Location Streaming Rules ─────────────────────────────────────
        // 1. CONFIRMED state: Broadcast live location from cache to Booker UI so booker tracks driver arrival
        // 2. VERIFIED state: Stop STOMP broadcast to Booker UI (booker is on bike), but KEEP saving location in Redis cache for Admin
        if (state == LiveRideState.CONFIRMED && ride.getBooker() != null) {
            LiveRideLocationBroadcast broadcast = LiveRideLocationBroadcast.builder()
                    .liveRideId(liveRideId)
                    .lat(driverLat)
                    .lng(driverLng)
                    .bearingDegrees(bearing)
                    .speedKmh(speed)
                    .timestamp(timestamp > 0 ? timestamp : System.currentTimeMillis())
                    .driverName(firstNameOnly(ride.getDriver().getFullName()))
                    .state(state)
                    .build();

            messagingTemplate.convertAndSend(String.format(LOCATION_TOPIC, liveRideId), broadcast);
        }

        // ── Remove Redis location key ONLY when ride is COMPLETED, CANCELLED, or EXPIRED ──
        if (state == LiveRideState.COMPLETED || state == LiveRideState.CANCELLED || state == LiveRideState.EXPIRED) {
            redisLocationCacheService.removeLiveRideLocation(liveRideId);
            return;
        }

        ScheduledRideLocationCacheService.RideLocationSnapshot snapshot = locationCacheService.getLocation(liveRideId);
        if (snapshot == null) return;

        long now = System.currentTimeMillis();

        // ── Unbooked Driver Expiration Rules (state == LIVE) ────────────────────
        if (state == LiveRideState.LIVE) {
            double destLat = ride.getToLat().doubleValue();
            double destLng = ride.getToLng().doubleValue();
            double distToDest = GeoUtil.distanceKm(driverLat, driverLng, destLat, destLng);

            // Rule A: Destination reached (within 0.5km or stayed near destination for >5 min)
            if (distToDest <= 0.5) {
                log.info("Unbooked live driver reached destination (distToDest={}km). Expiring liveRideId={}", distToDest, liveRideId);
                expireUnbookedLiveRide(ride, "Live ride expired as destination was reached.");
                return;
            }

            if (distToDest <= 2.0) {
                if (snapshot.getNearDropPointStartTime() == null) {
                    snapshot.setNearDropPointStartTime(now);
                } else if (now - snapshot.getNearDropPointStartTime() >= 5 * 60 * 1000L) {
                    log.info("Unbooked live driver stayed near destination for >5 min. Expiring liveRideId={}", liveRideId);
                    expireUnbookedLiveRide(ride, "Live ride expired as destination was reached.");
                    return;
                }
            } else {
                snapshot.setNearDropPointStartTime(null);
            }

            // Rule B: Passed destination and stayed out for 2 minutes
            GeoUtil.RouteProjection proj = GeoUtil.projectOntoRoute(
                    ride.getFromLat().doubleValue(), ride.getFromLng().doubleValue(),
                    destLat, destLng, driverLat, driverLng);

            if (proj.getT() > 1.0 && distToDest > 2.0) {
                if (snapshot.getPassedDestinationStartTime() == null) {
                    snapshot.setPassedDestinationStartTime(now);
                } else if (now - snapshot.getPassedDestinationStartTime() >= 2 * 60 * 1000L) {
                    log.info("Unbooked live driver passed destination for >2 min. Expiring liveRideId={}", liveRideId);
                    expireUnbookedLiveRide(ride, "Live ride expired as you passed the destination.");
                    return;
                }
            } else {
                snapshot.setPassedDestinationStartTime(null);
            }

            // Rule C: Stationary at the same location for 5 minutes
            if (snapshot.getStationaryStartTime() != null && (now - snapshot.getStationaryStartTime() >= 5 * 60 * 1000L)) {
                log.info("Unbooked live driver stationary at same location for >5 min. Expiring liveRideId={}", liveRideId);
                expireUnbookedLiveRide(ride, "Live ride expired due to 5 minutes of location inactivity.");
                return;
            }

            return; // No booker attached yet
        }

        // Only evaluate completion & off-route rules if ride is active with a booker (CONFIRMED or VERIFIED)
        if (state != LiveRideState.CONFIRMED && state != LiveRideState.VERIFIED) {
            return;
        }

        // ── Booked Ride Stationary Rule: 5 min at same location ────────────────
        if (snapshot.getStationaryStartTime() != null && (now - snapshot.getStationaryStartTime() >= 5 * 60 * 1000L)) {
            log.info("Booked live driver stationary for >5 min. Cancelling liveRideId={}", liveRideId);
            cancelLiveRide(ride, "Ride cancelled due to 5 minutes of driver location inactivity.");
            return;
        }

        Long bookerId = ride.getBooker() != null ? ride.getBooker().getId() : null;

        // Target drop coordinates (booker drop point if available, else driver toLat/Lng)
        double targetDropLat = ride.getDropLat() != null ? ride.getDropLat().doubleValue() : ride.getToLat().doubleValue();
        double targetDropLng = ride.getDropLng() != null ? ride.getDropLng().doubleValue() : ride.getToLng().doubleValue();

        // ── Rule 1: 1km Radius of Drop Point for 5 min Auto-Completion ─────────
        double distToDrop = GeoUtil.distanceKm(driverLat, driverLng, targetDropLat, targetDropLng);

        if (distToDrop <= NEAR_DROP_RADIUS_KM) {
            if (snapshot.getNearDropPointStartTime() == null) {
                snapshot.setNearDropPointStartTime(now);
                log.info("Live driver entered 1km radius of drop point for liveRideId={}", liveRideId);
            } else if (now - snapshot.getNearDropPointStartTime() >= NEAR_DROP_TIME_MS) {
                log.info("Live driver stayed near drop point for >5 min. Auto-completing liveRideId={}", liveRideId);
                autoCompleteLiveRide(ride, "Arrived at drop point.");
                return;
            }
        } else {
            snapshot.setNearDropPointStartTime(null);
        }

        // ── Rule 2: Off-Route Detection & 20 min Auto-Completion ─────────────
        double fromLat = ride.getFromLat().doubleValue();
        double fromLng = ride.getFromLng().doubleValue();
        double toLat = ride.getToLat().doubleValue();
        double toLng = ride.getToLng().doubleValue();

        GeoUtil.RouteProjection proj = GeoUtil.projectOntoRoute(fromLat, fromLng, toLat, toLng, driverLat, driverLng);
        double offRouteKm = proj.getOffRouteKm();

        if (offRouteKm > OFF_ROUTE_THRESHOLD_KM) {
            if (snapshot.getOffRouteStartTime() == null) {
                snapshot.setOffRouteStartTime(now);
            }

            if (!snapshot.isOffRouteAlertSent() && bookerId != null) {
                snapshot.setOffRouteAlertSent(true);
                fcmService.sendToUser(
                        bookerId,
                        "Route Warning",
                        "Route is not the selected one, please make sure your safety.",
                        Map.of("type", "LIVE_ROUTE_DEVIATION_ALERT", "liveRideId", String.valueOf(liveRideId))
                );
                log.warn("Off-route safety warning sent to bookerId={} for liveRideId={}", bookerId, liveRideId);
            }

            if (now - snapshot.getOffRouteStartTime() >= OFF_ROUTE_AUTO_COMPLETE_MS) {
                log.info("Live driver off route for >20 min. Auto-completing liveRideId={}", liveRideId);
                autoCompleteLiveRide(ride, "Completed due to extended route deviation.");
            }
        } else {
            snapshot.setOffRouteStartTime(null);
            snapshot.setOffRouteAlertSent(false);
        }
    }

    /**
     * Periodic background check for active live rides (reconciles DB & cache every 15s).
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void monitorActiveLiveRides() {
        List<LiveRide> activeRides = liveRideRepo.findAllActiveLiveRides();
        if (activeRides.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (LiveRide ride : activeRides) {
            Long liveRideId = ride.getId();
            try {
                // Check memory location cache or Redis
                ScheduledRideLocationCacheService.RideLocationSnapshot snapshot = locationCacheService.getLocation(liveRideId);
                RedisLocationCacheService.RedisLocationSnapshot redisSnap = null;
                if (snapshot == null) {
                    redisSnap = redisLocationCacheService.getLiveRideLocation(liveRideId);
                }

                long lastPingTs = snapshot != null ? snapshot.getTimestamp()
                        : (redisSnap != null ? redisSnap.getTimestamp() : 0);

                // If no location update received for > 5 minutes (300,000 ms)
                if (lastPingTs == 0 || (now - lastPingTs >= 5 * 60 * 1000L)) {
                    if (ride.getState() == LiveRideState.LIVE) {
                        log.info("LiveRide id={} had no location pings for >5 min. Expiring unbooked ride.", liveRideId);
                        expireUnbookedLiveRide(ride, "Live ride expired due to 5 minutes of location inactivity.");
                    } else if (ride.getState() == LiveRideState.CONFIRMED || ride.getState() == LiveRideState.VERIFIED) {
                        log.info("LiveRide id={} had no location pings for >5 min. Cancelling booked ride.", liveRideId);
                        cancelLiveRide(ride, "Ride cancelled due to 5 minutes of driver inactivity.");
                    }
                    continue;
                }

                if (snapshot != null) {
                    evaluateLiveRideLocation(liveRideId, snapshot.getLat(), snapshot.getLng(),
                            snapshot.getBearingDegrees(), snapshot.getSpeedKmh(), snapshot.getTimestamp());
                } else if (redisSnap != null) {
                    evaluateLiveRideLocation(liveRideId, redisSnap.getLat(), redisSnap.getLng(),
                            redisSnap.getBearingDegrees(), redisSnap.getSpeedKmh(), redisSnap.getTimestamp());
                }
            } catch (Exception e) {
                log.error("Error monitoring live ride location for liveRideId={}: {}", liveRideId, e.getMessage(), e);
            }
        }
    }

    private void autoCompleteLiveRide(LiveRide ride, String reason) {
        ride.setState(LiveRideState.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        liveRideRepo.save(ride);

        Long liveRideId = ride.getId();
        locationCacheService.removeLocation(liveRideId);
        redisLocationCacheService.removeLiveRideLocation(liveRideId);

        if (ride.getDriver() != null) {
            cacheService.removeLiveDriver(ride.getDriver().getId());
        }

        String driverName = ride.getDriver().getFullName();

        if (ride.getBooker() != null) {
            fcmService.sendToUser(
                    ride.getBooker().getId(),
                    "Live Ride Completed",
                    "Your live ride with " + driverName + " has been completed. (" + reason + ")",
                    Map.of("type", "LIVE_RIDE_COMPLETED", "liveRideId", String.valueOf(liveRideId))
            );
        }

        fcmService.sendToUser(
                ride.getDriver().getId(),
                "Live Ride Completed",
                "Your live ride has been completed.",
                Map.of("type", "LIVE_RIDE_COMPLETED", "liveRideId", String.valueOf(liveRideId))
        );

        log.info("LiveRide id={} auto-completed. Reason: {}", liveRideId, reason);
    }

    private void cancelLiveRide(LiveRide ride, String reason) {
        ride.setState(LiveRideState.CANCELLED);
        ride.setCancelledAt(LocalDateTime.now());
        liveRideRepo.save(ride);

        Long liveRideId = ride.getId();
        locationCacheService.removeLocation(liveRideId);
        redisLocationCacheService.removeLiveRideLocation(liveRideId);

        if (ride.getDriver() != null) {
            cacheService.removeLiveDriver(ride.getDriver().getId());
            fcmService.sendToUser(
                    ride.getDriver().getId(),
                    "Live Ride Cancelled",
                    reason,
                    Map.of("type", "LIVE_RIDE_CANCELLED", "liveRideId", String.valueOf(liveRideId))
            );
        }

        if (ride.getBooker() != null) {
            fcmService.sendToUser(
                    ride.getBooker().getId(),
                    "Live Ride Cancelled",
                    reason,
                    Map.of("type", "LIVE_RIDE_CANCELLED", "liveRideId", String.valueOf(liveRideId))
            );
        }

        log.info("LiveRide id={} cancelled. Reason: {}", liveRideId, reason);
    }

    private void expireUnbookedLiveRide(LiveRide ride, String reason) {
        ride.setState(LiveRideState.EXPIRED);
        ride.setCancelledAt(LocalDateTime.now());
        liveRideRepo.save(ride);

        Long liveRideId = ride.getId();
        locationCacheService.removeLocation(liveRideId);
        redisLocationCacheService.removeLiveRideLocation(liveRideId);

        if (ride.getDriver() != null) {
            cacheService.removeLiveDriver(ride.getDriver().getId());
            fcmService.sendToUser(
                    ride.getDriver().getId(),
                    "Live Mode Expired",
                    reason,
                    Map.of("type", "LIVE_RIDE_EXPIRED", "liveRideId", String.valueOf(liveRideId))
            );
        }

        log.info("Unbooked LiveRide id={} expired. Reason: {}", liveRideId, reason);
    }

    private static String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Driver";
        return fullName.trim().split("\\s+")[0];
    }
}
