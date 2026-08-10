package com.bikepooling.service;

import com.bikepooling.entity.ScheduledRideApplication;
import com.bikepooling.entity.ScheduledRideInstance;
import com.bikepooling.entity.ScheduledRideTemplate;
import com.bikepooling.enums.RideState;
import com.bikepooling.repository.ScheduledRideApplicationDayRepository;
import com.bikepooling.repository.ScheduledRideInstanceRepository;
import com.bikepooling.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class ScheduledRideTrackingService {

    private final ScheduledRideLocationCacheService locationCacheService;
    private final RedisLocationCacheService redisLocationCacheService;
    private final ScheduledRideInstanceRepository instanceRepo;
    private final ScheduledRideApplicationDayRepository appDayRepo;
    private final FcmService fcmService;

    private static final double NEAR_DROP_RADIUS_KM = 1.0;            // 1km radius near drop point
    private static final long   NEAR_DROP_TIME_MS   = 5 * 60 * 1000L;  // 5 minutes
    private static final double OFF_ROUTE_THRESHOLD_KM = 2.0;          // 2km off-route threshold
    private static final long   OFF_ROUTE_AUTO_COMPLETE_MS = 20 * 60 * 1000L; // 20 minutes

    /**
     * Called whenever a driver pushes a new GPS location via WebSocket STOMP.
     */
    public void processDriverLocation(Long instanceId, double lat, double lng, Double bearing, Double speed, long timestamp, Long driverId) {
        // 1. Fetch current instance state
        String stateStr = "STARTED";
        Optional<ScheduledRideInstance> instOpt = instanceRepo.findById(instanceId);
        if (instOpt.isPresent()) {
            stateStr = instOpt.get().getState().name();
        }

        // 2. Save location ping into Redis Cache (key: live:scheduled_ride:{instanceId})
        redisLocationCacheService.saveScheduledRideLocation(instanceId, driverId, lat, lng, bearing, speed, timestamp, stateStr);

        // 3. Update in-memory location cache for local safety evaluation
        locationCacheService.updateLocation(instanceId, lat, lng, bearing, speed, timestamp, driverId);

        // 4. Evaluate auto-completion & route safety rules
        evaluateLocationForInstance(instanceId, lat, lng);
    }

    /**
     * Periodic background check every 15 seconds for active tracked rides.
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void monitorActiveRides() {
        List<ScheduledRideInstance> activeInstances = instanceRepo.findAllActiveInstances();
        if (activeInstances.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (ScheduledRideInstance inst : activeInstances) {
            Long instanceId = inst.getId();
            try {
                ScheduledRideLocationCacheService.RideLocationSnapshot snapshot = locationCacheService.getLocation(instanceId);
                RedisLocationCacheService.RedisLocationSnapshot redisSnap = null;
                if (snapshot == null) {
                    redisSnap = redisLocationCacheService.getScheduledRideLocation(instanceId);
                }

                long lastPingTs = snapshot != null ? snapshot.getTimestamp()
                        : (redisSnap != null ? redisSnap.getTimestamp() : 0);

                // If no location update received for > 5 minutes (300,000 ms)
                if (lastPingTs == 0 || (now - lastPingTs >= 5 * 60 * 1000L)) {
                    log.info("ScheduledRideInstance instanceId={} had no location pings for >5 min. Cancelling ride.", instanceId);
                    cancelRideInstance(inst, "Scheduled ride cancelled due to 5 minutes of driver location inactivity.");
                    continue;
                }

                if (snapshot != null) {
                    evaluateLocationForInstance(instanceId, snapshot.getLat(), snapshot.getLng());
                } else if (redisSnap != null) {
                    evaluateLocationForInstance(instanceId, redisSnap.getLat(), redisSnap.getLng());
                }
            } catch (Exception e) {
                log.error("Error monitoring location for instanceId={}: {}", instanceId, e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void evaluateLocationForInstance(Long instanceId, double driverLat, double driverLng) {
        Optional<ScheduledRideInstance> instOpt = instanceRepo.findByIdWithDetails(instanceId);
        if (instOpt.isEmpty()) {
            locationCacheService.removeLocation(instanceId);
            redisLocationCacheService.removeScheduledRideLocation(instanceId);
            return;
        }

        ScheduledRideInstance inst = instOpt.get();
        RideState state = inst.getState();

        // Only evaluate if ride is active (STARTED, VERIFIED, or SOS_TRIGGERED)
        if (state != RideState.STARTED && state != RideState.VERIFIED && state != RideState.SOS_TRIGGERED) {
            locationCacheService.removeLocation(instanceId);
            redisLocationCacheService.removeScheduledRideLocation(instanceId);
            return;
        }

        ScheduledRideLocationCacheService.RideLocationSnapshot snapshot = locationCacheService.getLocation(instanceId);
        if (snapshot == null) return;

        long now = System.currentTimeMillis();

        // Check if stationary for >= 5 minutes
        if (snapshot.getStationaryStartTime() != null && (now - snapshot.getStationaryStartTime() >= 5 * 60 * 1000L)) {
            log.info("Scheduled ride driver stationary for >5 min. Cancelling instanceId={}", instanceId);
            cancelRideInstance(inst, "Scheduled ride cancelled due to 5 minutes of driver inactivity.");
            return;
        }

        ScheduledRideTemplate template = inst.getTemplate();

        // Determine drop point (booker's specific drop point if booked, else template drop point)
        double targetDropLat = template.getToLat().doubleValue();
        double targetDropLng = template.getToLng().doubleValue();
        Long bookerId = inst.getBookedBy() != null ? inst.getBookedBy().getId() : null;

        if (inst.getBookedBy() != null) {
            var activeDays = appDayRepo.findActiveByTemplateId(template.getId());
            for (var day : activeDays) {
                if (day.getInstance().getId().equals(instanceId) && day.getApplication().getBooker().getId().equals(bookerId)) {
                    ScheduledRideApplication app = day.getApplication();
                    if (app.getDropLat() != null && app.getDropLng() != null) {
                        targetDropLat = app.getDropLat().doubleValue();
                        targetDropLng = app.getDropLng().doubleValue();
                    }
                    break;
                }
            }
        }

        // ── Rule 1: 1km Radius of Drop Point for 5 min Auto-Completion ─────────
        double distToDrop = GeoUtil.distanceKm(driverLat, driverLng, targetDropLat, targetDropLng);

        if (distToDrop <= NEAR_DROP_RADIUS_KM) {
            if (snapshot.getNearDropPointStartTime() == null) {
                snapshot.setNearDropPointStartTime(now);
                log.info("Driver entered {}km radius of drop point for instanceId={}", NEAR_DROP_RADIUS_KM, instanceId);
            } else if (now - snapshot.getNearDropPointStartTime() >= NEAR_DROP_TIME_MS) {
                log.info("Driver stayed near drop point for >5 min. Auto-completing ride instanceId={}", instanceId);
                autoCompleteRide(inst, "Driver arrived at drop point.");
                return;
            }
        } else {
            snapshot.setNearDropPointStartTime(null);
        }

        // ── Rule 2: Off-Route Detection & 20 min Auto-Completion ─────────────
        double fromLat = template.getFromLat().doubleValue();
        double fromLng = template.getFromLng().doubleValue();
        double toLat = template.getToLat().doubleValue();
        double toLng = template.getToLng().doubleValue();

        GeoUtil.RouteProjection proj = GeoUtil.projectOntoRoute(fromLat, fromLng, toLat, toLng, driverLat, driverLng);
        double offRouteKm = proj.getOffRouteKm();

        if (offRouteKm > OFF_ROUTE_THRESHOLD_KM) {
            if (snapshot.getOffRouteStartTime() == null) {
                snapshot.setOffRouteStartTime(now);
            }

            // Send safety notification to booker once
            if (!snapshot.isOffRouteAlertSent() && bookerId != null) {
                snapshot.setOffRouteAlertSent(true);
                fcmService.sendToUser(
                        bookerId,
                        "Route Warning",
                        "Route is not the selected one, please make sure your safety.",
                        Map.of("type", "ROUTE_DEVIATION_ALERT", "instanceId", String.valueOf(instanceId))
                );
                log.warn("Route deviation safety notification sent to bookerId={} for instanceId={} (offRouteKm={})",
                        bookerId, instanceId, offRouteKm);
            }

            // If off-route for >= 20 minutes, stop location sharing & auto-complete ride
            if (now - snapshot.getOffRouteStartTime() >= OFF_ROUTE_AUTO_COMPLETE_MS) {
                log.info("Driver off route for >20 min. Auto-completing ride instanceId={}", instanceId);
                autoCompleteRide(inst, "Ride completed due to extended route deviation (20 min off-route).");
            }
        } else {
            snapshot.setOffRouteStartTime(null);
            snapshot.setOffRouteAlertSent(false);
        }
    }

    private void autoCompleteRide(ScheduledRideInstance inst, String reason) {
        inst.setState(RideState.COMPLETED);
        inst.setCompletedAt(LocalDateTime.now());
        instanceRepo.save(inst);

        Long instanceId = inst.getId();
        locationCacheService.removeLocation(instanceId);
        redisLocationCacheService.removeScheduledRideLocation(instanceId);

        String driverName = inst.getTemplate().getPostedBy().getFullName();

        if (inst.getBookedBy() != null) {
            Long bookerId = inst.getBookedBy().getId();
            fcmService.sendToUser(
                    bookerId,
                    "Ride Completed",
                    "Your ride with " + driverName + " has been completed. (" + reason + ")",
                    Map.of("type", "SCHEDULED_RIDE_COMPLETED", "instanceId", String.valueOf(instanceId))
            );
        }

        fcmService.sendToUser(
                inst.getTemplate().getPostedBy().getId(),
                "Ride Completed",
                "Your scheduled ride has been automatically marked as completed.",
                Map.of("type", "SCHEDULED_RIDE_COMPLETED", "instanceId", String.valueOf(instanceId))
            );

        log.info("Ride instanceId={} auto-completed. Reason: {}", instanceId, reason);
    }

    private void cancelRideInstance(ScheduledRideInstance inst, String reason) {
        inst.setState(RideState.CANCELLED);
        inst.setCancelledAt(LocalDateTime.now());
        instanceRepo.save(inst);

        Long instanceId = inst.getId();
        locationCacheService.removeLocation(instanceId);
        redisLocationCacheService.removeScheduledRideLocation(instanceId);

        if (inst.getTemplate() != null && inst.getTemplate().getPostedBy() != null) {
            fcmService.sendToUser(
                    inst.getTemplate().getPostedBy().getId(),
                    "Ride Cancelled",
                    reason,
                    Map.of("type", "SCHEDULED_RIDE_CANCELLED", "instanceId", String.valueOf(instanceId))
            );
        }

        if (inst.getBookedBy() != null) {
            fcmService.sendToUser(
                    inst.getBookedBy().getId(),
                    "Ride Cancelled",
                    reason,
                    Map.of("type", "SCHEDULED_RIDE_CANCELLED", "instanceId", String.valueOf(instanceId))
            );
        }

        log.info("ScheduledRideInstance instanceId={} cancelled. Reason: {}", instanceId, reason);
    }
}
