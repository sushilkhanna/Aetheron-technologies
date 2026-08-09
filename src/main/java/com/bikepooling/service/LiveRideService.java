package com.bikepooling.service;

import com.bikepooling.config.OsrmClient;
import com.bikepooling.dto.request.LiveRideLocation;
import com.bikepooling.dto.request.LiveRideMeta;
import com.bikepooling.entity.Ride;
import com.bikepooling.entity.RideApplication;
import com.bikepooling.entity.RideStatus;
import com.bikepooling.entity.User;
import com.bikepooling.enums.ApplicationStatus;
import com.bikepooling.enums.RideState;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.LiveRideRedisRepository;
import com.bikepooling.repository.RideApplicationRepository;
import com.bikepooling.repository.RideRepository;
import com.bikepooling.repository.RideStatusRepository;
import com.bikepooling.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Owns all Redis state and ride state transitions.
 * Controller owns WebSocket broadcast — SimpMessagingTemplate is NOT injected here.
 *
 * Redis is now split into LiveRideMeta (rare writes: goLive, booking, verify)
 * and LiveRideLocation (every ~3s tick) so each ping only rewrites the small
 * location payload instead of the entire ride object.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRideService {

    private static final double END_RADIUS_KM                = 0.2;
    private static final double PASS_THROUGH_EXIT_RADIUS_KM  = 0.4;
    private static final long   LIVE_DWELL_THRESHOLD_MS      = 30_000;
    private static final long   VERIFIED_DWELL_THRESHOLD_MS  = 30_000;
    private static final int    TIME_EXCEEDED_BUFFER_MIN     = 15;

    private final LiveRideRedisRepository   liveRideRepo;
    private final RideRepository            rideRepo;
    private final RideStatusRepository      rideStatusRepo;
    private final RideApplicationRepository applicationRepo;
    private final FcmService                fcmService;
    private final OsrmClient                osrmClient;

    // ── Go Live ────────────────────────────────────────────────────────────────

    @Transactional
    public void goLive(Long rideId, Long driverId) {

        Long existingLiveRide = liveRideRepo.findRideIdByDriverId(driverId);
        if (existingLiveRide != null) {
            throw AppException.conflict(
                    "You are already live on ride #" + existingLiveRide + ". Go offline first.");
        }

        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));
        Ride ride = status.getRide();

        if (!ride.getPostedBy().getId().equals(driverId)) {
            throw AppException.forbidden("You can only go live on your own ride.");
        }
        if (status.getState() != RideState.OPEN) {
            throw AppException.conflict(
                    "Only OPEN rides can go live. Current state: " + status.getState());
        }

        cancelPendingApplicationsForLive(rideId, ride);

        int estimatedMinutes = osrmClient.getRoadDurationMinutes(
                ride.getFromLat().doubleValue(), ride.getFromLng().doubleValue(),
                ride.getToLat().doubleValue(),   ride.getToLng().doubleValue());

        status.setState(RideState.LIVE);
        rideStatusRepo.save(status);

        long now = System.currentTimeMillis();

        LiveRideMeta meta = LiveRideMeta.builder()
                .rideId(rideId)
                .driverId(driverId)
                .vehicleNumber(ride.getVehicle().getVehicleNumber())
                .fromLat(ride.getFromLat().doubleValue())
                .fromLng(ride.getFromLng().doubleValue())
                .toLat(ride.getToLat().doubleValue())
                .toLng(ride.getToLng().doubleValue())
                .goLiveLat(ride.getFromLat().doubleValue())
                .goLiveLng(ride.getFromLng().doubleValue())
                .bookerPickupLat(0)
                .bookerPickupLng(0)
                .bookerDropLat(0)
                .bookerDropLng(0)
                .bookerDropSet(false)
                .distanceKm(ride.getDistanceKm().doubleValue())
                .extraDistanceKm(ride.getExtraDistanceKm().doubleValue())
                .preferredGender(ride.getPreferredGender().name())
                .paymentMode(ride.getPaymentMode().name())
                .goLiveAt(now)
                .estimatedDurationMinutes(estimatedMinutes)
                .remainingDurationMinutes(0)
                .verifiedStartAt(0)
                .currentState(RideState.LIVE.name())
                .build();

        LiveRideLocation loc = LiveRideLocation.builder()
                .rideId(rideId)
                .currentLat(ride.getFromLat().doubleValue())
                .currentLng(ride.getFromLng().doubleValue())
                .lastUpdatedAt(now)
                .currentDwellStartedAt(0)
                .cumulativeDwellMs(0)
                .minDistToDestKm(Double.MAX_VALUE)
                .build();

        liveRideRepo.saveMeta(meta);
        liveRideRepo.saveLocation(loc);

        log.info("Driver LIVE: driverId={} rideId={} estimatedMin={}",
                driverId, rideId, estimatedMinutes);
    }

    // ── Seed Redis for pre-posted STARTED flow ─────────────────────────────────

    public void seedRedisForStartedRide(Long rideId, Long driverId,
                                        double bookerPickupLat, double bookerPickupLng,
                                        double bookerDropLat,   double bookerDropLng) {

        LiveRideMeta existing = liveRideRepo.findMeta(rideId);
        if (existing != null) {
            existing.setBookerPickupLat(bookerPickupLat);
            existing.setBookerPickupLng(bookerPickupLng);
            existing.setBookerDropLat(bookerDropLat);
            existing.setBookerDropLng(bookerDropLng);
            existing.setBookerDropSet(true);
            existing.setCurrentState(RideState.STARTED.name());
            liveRideRepo.saveMeta(existing);
            log.info("Redis meta already existed (live flow), updated booker points: rideId={}", rideId);
            return;
        }

        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));
        Ride ride = status.getRide();

        int estimatedMinutes = osrmClient.getRoadDurationMinutes(
                ride.getFromLat().doubleValue(), ride.getFromLng().doubleValue(),
                ride.getToLat().doubleValue(),   ride.getToLng().doubleValue());

        long now = System.currentTimeMillis();

        LiveRideMeta meta = LiveRideMeta.builder()
                .rideId(rideId)
                .driverId(driverId)
                .vehicleNumber(ride.getVehicle().getVehicleNumber())
                .fromLat(ride.getFromLat().doubleValue())
                .fromLng(ride.getFromLng().doubleValue())
                .toLat(ride.getToLat().doubleValue())
                .toLng(ride.getToLng().doubleValue())
                .goLiveLat(ride.getFromLat().doubleValue())
                .goLiveLng(ride.getFromLng().doubleValue())
                .bookerPickupLat(bookerPickupLat)
                .bookerPickupLng(bookerPickupLng)
                .bookerDropLat(bookerDropLat)
                .bookerDropLng(bookerDropLng)
                .bookerDropSet(true)
                .distanceKm(ride.getDistanceKm().doubleValue())
                .extraDistanceKm(ride.getExtraDistanceKm().doubleValue())
                .preferredGender(ride.getPreferredGender().name())
                .paymentMode(ride.getPaymentMode().name())
                .goLiveAt(now)
                .estimatedDurationMinutes(estimatedMinutes)
                .remainingDurationMinutes(0)
                .verifiedStartAt(0)
                .currentState(RideState.STARTED.name())
                .build();

        LiveRideLocation loc = LiveRideLocation.builder()
                .rideId(rideId)
                .currentLat(ride.getFromLat().doubleValue())
                .currentLng(ride.getFromLng().doubleValue())
                .lastUpdatedAt(now)
                .currentDwellStartedAt(0)
                .cumulativeDwellMs(0)
                .minDistToDestKm(Double.MAX_VALUE)
                .build();

        liveRideRepo.saveMeta(meta);
        liveRideRepo.saveLocation(loc);

        log.info("Redis seeded for pre-posted STARTED ride: rideId={} driverId={}", rideId, driverId);
    }

    // ── Update Location ────────────────────────────────────────────────────────

    /**
     * Called every ~3 sec from LiveLocationController.
     * Only rewrites the LiveRideLocation key — LiveRideMeta is untouched here.
     */
    @Transactional
    public LiveRideLocation updateLocation(Long rideId, Long driverId,
                                           double lat, double lng,
                                           double bearingDegrees, double speedKmh,
                                           RideState currentState) {
        LiveRideMeta meta = liveRideRepo.findMeta(rideId);
        if (meta == null) return null;
        if (!meta.getDriverId().equals(driverId)) return null;

        long now = System.currentTimeMillis();

        if (currentState == RideState.LIVE) {
            long maxMs = (long) (meta.getEstimatedDurationMinutes() + TIME_EXCEEDED_BUFFER_MIN) * 60_000L;
            if (now - meta.getGoLiveAt() > maxMs) {
                log.info("LIVE: ETA+{}min exceeded — expiring: rideId={}",
                        TIME_EXCEEDED_BUFFER_MIN, rideId);
                expireLiveRide(rideId, driverId);
                return null;
            }
        }

        LiveRideLocation loc = liveRideRepo.findLocation(rideId);
        if (loc == null) {
            loc = LiveRideLocation.builder()
                    .rideId(rideId)
                    .currentDwellStartedAt(0)
                    .cumulativeDwellMs(0)
                    .minDistToDestKm(Double.MAX_VALUE)
                    .build();
        }
        loc.setCurrentLat(lat);
        loc.setCurrentLng(lng);
        loc.setBearingDegrees(bearingDegrees);
        loc.setSpeedKmh(speedKmh);
        loc.setLastUpdatedAt(now);

        if (currentState == RideState.LIVE) {
            if (checkLiveDwell(meta, loc, lat, lng, now)) return null;
        } else if (currentState == RideState.VERIFIED) {
            if (checkVerifiedDwell(meta, loc, lat, lng, now)) return null;
        }

        liveRideRepo.saveLocation(loc);
        return loc;
    }

    // ── On Ride Confirmed in Live Flow (LIVE → STARTED) ───────────────────────

    public void onRideBooked(Long rideId,
                             double bookerPickupLat, double bookerPickupLng,
                             double bookerDropLat,   double bookerDropLng) {
        LiveRideMeta meta = liveRideRepo.findMeta(rideId);
        if (meta == null) return;

        meta.setBookerPickupLat(bookerPickupLat);
        meta.setBookerPickupLng(bookerPickupLng);
        meta.setBookerDropLat(bookerDropLat);
        meta.setBookerDropLng(bookerDropLng);
        meta.setBookerDropSet(true);
        meta.setCurrentState(RideState.STARTED.name());
        liveRideRepo.saveMeta(meta);

        log.info("Live ride confirmed → STARTED, booker points + state set in Redis: rideId={}", rideId);
    }

    // ── On Ride Verified ────────────────────────────────────────────────────────

    public void onRideVerified(Long rideId) {
        LiveRideMeta meta = liveRideRepo.findMeta(rideId);
        if (meta == null) {
            log.warn("onRideVerified: no Redis meta found for rideId={}", rideId);
            return;
        }

        int remainingMinutes = 0;
        if (meta.isBookerDropSet()) {
            try {
                remainingMinutes = osrmClient.getRoadDurationMinutes(
                        meta.getBookerPickupLat(), meta.getBookerPickupLng(),
                        meta.getBookerDropLat(),   meta.getBookerDropLng());
            } catch (Exception e) {
                log.warn("onRideVerified: OSRM call failed for rideId={}, " +
                        "falling back to estimatedDurationMinutes", rideId, e);
                remainingMinutes = meta.getEstimatedDurationMinutes();
            }
        }

        meta.setRemainingDurationMinutes(remainingMinutes);
        meta.setVerifiedStartAt(System.currentTimeMillis());
        meta.setCurrentState(RideState.VERIFIED.name());
        liveRideRepo.saveMeta(meta);

        LiveRideLocation loc = liveRideRepo.findLocation(rideId);
        if (loc == null) {
            loc = LiveRideLocation.builder().rideId(rideId).build();
        }
        loc.setCurrentDwellStartedAt(0);
        loc.setCumulativeDwellMs(0);
        loc.setMinDistToDestKm(Double.MAX_VALUE);
        liveRideRepo.saveLocation(loc);

        log.info("Ride VERIFIED — dwell reset, remainingMin={}, timeout started: rideId={}",
                remainingMinutes, rideId);
    }

    // ── Go Offline ─────────────────────────────────────────────────────────────

    @Transactional
    public void goOffline(Long rideId, Long driverId) {
        LiveRideMeta meta = liveRideRepo.findMeta(rideId);
        if (meta == null) {
            log.info("goOffline: rideId={} already removed from Redis", rideId);
            return;
        }
        if (!meta.getDriverId().equals(driverId)) {
            throw AppException.forbidden("You cannot end someone else's live ride.");
        }

        rideStatusRepo.findByRideIdWithDetails(rideId).ifPresent(status -> {
            if (status.getState() == RideState.LIVE) {
                status.setState(RideState.OPEN);
                rideStatusRepo.save(status);
                log.info("Ride state reverted LIVE → OPEN: rideId={}", rideId);
            }
        });

        liveRideRepo.delete(rideId, driverId);
    }

    // ── Private: dwell checkers ────────────────────────────────────────────────

    private boolean checkLiveDwell(LiveRideMeta meta, LiveRideLocation loc,
                                   double lat, double lng, long now) {
        double distKm = GeoUtil.distanceKm(lat, lng, meta.getToLat(), meta.getToLng());

        if (distKm < loc.getMinDistToDestKm()) {
            loc.setMinDistToDestKm(distKm);
        }

        if (distKm <= END_RADIUS_KM) {
            if (loc.getCurrentDwellStartedAt() == 0) {
                loc.setCurrentDwellStartedAt(now);
            } else {
                long tickMs = now - loc.getCurrentDwellStartedAt();
                loc.setCumulativeDwellMs(loc.getCumulativeDwellMs() + tickMs);
                loc.setCurrentDwellStartedAt(now);

                if (loc.getCumulativeDwellMs() >= LIVE_DWELL_THRESHOLD_MS) {
                    log.info("LIVE: 30s dwell at destination — expiring: rideId={}", meta.getRideId());
                    expireLiveRide(meta.getRideId(), meta.getDriverId());
                    return true;
                }
            }
        } else {
            loc.setCurrentDwellStartedAt(0);

            if (loc.getMinDistToDestKm() <= END_RADIUS_KM && distKm > PASS_THROUGH_EXIT_RADIUS_KM) {
                log.info("LIVE: passed destination — expiring: rideId={} minDist={}km now={}km",
                        meta.getRideId(), loc.getMinDistToDestKm(), distKm);
                expireLiveRide(meta.getRideId(), meta.getDriverId());
                return true;
            }
        }

        return false;
    }

    private boolean checkVerifiedDwell(LiveRideMeta meta, LiveRideLocation loc,
                                       double lat, double lng, long now) {
        if (!meta.isBookerDropSet()) return false;

        double distKm = GeoUtil.distanceKm(lat, lng, meta.getBookerDropLat(), meta.getBookerDropLng());

        if (distKm < loc.getMinDistToDestKm()) {
            loc.setMinDistToDestKm(distKm);
        }

        if (distKm <= END_RADIUS_KM) {
            if (loc.getCurrentDwellStartedAt() == 0) {
                loc.setCurrentDwellStartedAt(now);
            } else {
                long tickMs = now - loc.getCurrentDwellStartedAt();
                loc.setCumulativeDwellMs(loc.getCumulativeDwellMs() + tickMs);
                loc.setCurrentDwellStartedAt(now);

                if (loc.getCumulativeDwellMs() >= VERIFIED_DWELL_THRESHOLD_MS) {
                    log.info("VERIFIED: 30s dwell at drop — completing: rideId={}", meta.getRideId());
                    completeVerifiedRide(meta.getRideId(), meta.getDriverId());
                    return true;
                }
            }
        } else {
            loc.setCurrentDwellStartedAt(0);

            if (loc.getMinDistToDestKm() <= END_RADIUS_KM && distKm > PASS_THROUGH_EXIT_RADIUS_KM) {
                log.info("VERIFIED: passed drop point — completing: rideId={} minDist={}km now={}km",
                        meta.getRideId(), loc.getMinDistToDestKm(), distKm);
                completeVerifiedRide(meta.getRideId(), meta.getDriverId());
                return true;
            }
        }

        if (meta.getVerifiedStartAt() > 0 && meta.getRemainingDurationMinutes() > 0) {
            long maxMs = (long) (meta.getRemainingDurationMinutes() + TIME_EXCEEDED_BUFFER_MIN) * 60_000L;
            if (now - meta.getVerifiedStartAt() > maxMs) {
                log.info("VERIFIED: remainingETA+{}min exceeded — completing: rideId={}",
                        TIME_EXCEEDED_BUFFER_MIN, meta.getRideId());
                completeVerifiedRide(meta.getRideId(), meta.getDriverId());
                return true;
            }
        }

        return false;
    }

    // ── Private: state transitions ─────────────────────────────────────────────

    protected void expireLiveRide(Long rideId, Long driverId) {
        rideStatusRepo.findByRideIdWithDetails(rideId).ifPresent(status -> {
            if (status.getState() == RideState.LIVE) {
                status.setState(RideState.EXPIRED);
                rideStatusRepo.save(status);
                log.info("Ride LIVE → EXPIRED: rideId={}", rideId);
            }
        });
        liveRideRepo.delete(rideId, driverId);
    }

    protected void completeVerifiedRide(Long rideId, Long driverId) {
        rideStatusRepo.findByRideIdWithDetails(rideId).ifPresent(status -> {
            if (status.getState() == RideState.VERIFIED) {
                status.setState(RideState.COMPLETED);
                status.setCompletedAt(LocalDateTime.now());
                rideStatusRepo.save(status);

                User booker = status.getBookedBy();
                if (booker != null) {
                    fcmService.notifyBookerRideCompleted(
                            booker.getId(),
                            status.getRide().getPostedBy().getFullName(),
                            rideId);
                }
                log.info("Ride auto-COMPLETED: rideId={}", rideId);
            }
        });
        liveRideRepo.delete(rideId, driverId);
    }

    private void cancelPendingApplicationsForLive(Long rideId, Ride ride) {
        List<RideApplication> pending = applicationRepo.findPendingByRideId(rideId);
        for (RideApplication app : pending) {
            app.setStatus(ApplicationStatus.REJECTED);
            app.setDeleted(true);
            app.setDeletedAt(LocalDateTime.now());
            applicationRepo.save(app);
            fcmService.notifyBookerApplicationRejectedDriverWentLive(
                    app.getBooker().getId(),
                    ride.getPostedBy().getFullName(),
                    rideId);
            log.info("Cancelled pending app id={} bookerId={} — driver went live rideId={}",
                    app.getId(), app.getBooker().getId(), rideId);
        }
    }
}