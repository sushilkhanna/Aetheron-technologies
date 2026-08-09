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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledRideTrackingService {

    private final ScheduledRideLocationCacheService locationCacheService;
    private final ScheduledRideInstanceRepository instanceRepo;
    private final ScheduledRideApplicationDayRepository appDayRepo;
    private final FcmService fcmService;

    private static final double NEAR_DROP_RADIUS_KM = 2.0;
    private static final long   NEAR_DROP_TIME_MS   = 5 * 60 * 1000L;  // 5 minutes
    private static final double OFF_ROUTE_THRESHOLD_KM = 3.0;
    private static final long   OFF_ROUTE_AUTO_COMPLETE_MS = 30 * 60 * 1000L; // 30 minutes

    /**
     * Called whenever a driver pushes a new GPS location.
     */
    public void processDriverLocation(Long instanceId, double lat, double lng, Double bearing, Double speed, long timestamp, Long driverId) {
        // 1. Update in-memory location cache
        locationCacheService.updateLocation(instanceId, lat, lng, bearing, speed, timestamp, driverId);

        // 2. Evaluate ride instance metrics asynchronously / inline
        evaluateLocationForInstance(instanceId, lat, lng);
    }

    /**
     * Periodic background check every 30 seconds for active tracked rides.
     */
    @Scheduled(fixedRate = 30000)
    public void monitorActiveRides() {
        Map<Long, ScheduledRideLocationCacheService.RideLocationSnapshot> snapshots = locationCacheService.getAllSnapshots();
        if (snapshots.isEmpty()) return;

        log.debug("Monitoring {} active cached rides...", snapshots.size());
        for (var entry : snapshots.entrySet()) {
            Long instanceId = entry.getKey();
            var snapshot = entry.getValue();
            try {
                evaluateLocationForInstance(instanceId, snapshot.getLat(), snapshot.getLng());
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
            return;
        }

        ScheduledRideInstance inst = instOpt.get();
        RideState state = inst.getState();

        // Only evaluate if ride is active (STARTED or VERIFIED)
        if (state != RideState.STARTED && state != RideState.VERIFIED && state != RideState.SOS_TRIGGERED) {
            locationCacheService.removeLocation(instanceId);
            return;
        }

        ScheduledRideLocationCacheService.RideLocationSnapshot snapshot = locationCacheService.getLocation(instanceId);
        if (snapshot == null) return;

        ScheduledRideTemplate template = inst.getTemplate();
        long now = System.currentTimeMillis();

        // Determine drop point (booker's specific drop point if booked, else template drop point)
        double targetDropLat = template.getToLat().doubleValue();
        double targetDropLng = template.getToLng().doubleValue();
        Long bookerId = inst.getBookedBy() != null ? inst.getBookedBy().getId() : null;

        // Check if booker has custom drop coordinates
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

        // ── Rule 1: Distance to Drop Point ─────────────────────────────────────
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
            // Driver is not near drop point
            snapshot.setNearDropPointStartTime(null);
        }

        // ── Rule 2 & 3: Route Deviation Monitoring ──────────────────────────────
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
                        "Route Deviation Alert",
                        "Driver " + template.getPostedBy().getFullName() + " is off the planned route. Please check your map, share your location with loved ones, and stay safe.",
                        Map.of("type", "ROUTE_DEVIATION_ALERT", "instanceId", String.valueOf(instanceId))
                );
                log.warn("Route deviation alert sent to bookerId={} for instanceId={} (offRouteKm={})",
                        bookerId, instanceId, offRouteKm);
            }

            // If off-route for >= 30 minutes, auto-complete ride
            if (now - snapshot.getOffRouteStartTime() >= OFF_ROUTE_AUTO_COMPLETE_MS) {
                log.info("Driver off route for >30 min. Auto-completing ride instanceId={}", instanceId);
                autoCompleteRide(inst, "Ride completed due to extended route deviation.");
            }
        } else {
            // Driver back on route
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
}
