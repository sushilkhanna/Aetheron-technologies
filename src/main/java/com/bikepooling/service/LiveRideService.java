package com.bikepooling.service;

import com.bikepooling.dto.request.*;
import com.bikepooling.dto.response.LiveRidePreviewResponse;
import com.bikepooling.dto.response.LiveRideResponse;
import com.bikepooling.entity.LiveRide;
import com.bikepooling.entity.User;
import com.bikepooling.entity.Vehicle;
import com.bikepooling.enums.LiveRideState;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.LiveRideRepository;
import com.bikepooling.repository.UserRepository;
import com.bikepooling.repository.VehicleRepository;
import com.bikepooling.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRideService {

    private final LiveRideRepository liveRideRepo;
    private final UserRepository userRepo;
    private final VehicleRepository vehicleRepo;
    private final LiveRideCacheService cacheService;
    private final ScheduledRideLocationCacheService locationCacheService;
    private final RedisLocationCacheService redisLocationCacheService;
    private final FcmService fcmService;

    private static final BigDecimal PER_KM_RATE = BigDecimal.valueOf(6.0);
    private static final BigDecimal MIN_FARE = BigDecimal.valueOf(25.0);

    @Transactional
    public LiveRideResponse goLive(Long driverId, GoLiveRequest req) {
        User driver = userRepo.findById(driverId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        Vehicle vehicle = null;
        if (req.getVehicleId() != null) {
            vehicle = vehicleRepo.findById(req.getVehicleId()).orElse(null);
        }
        if (vehicle == null) {
            vehicle = vehicleRepo.findByUserIdAndActiveTrue(driverId).orElse(null);
        }

        // Cancel any existing active live ride for this driver
        List<LiveRide> existing = liveRideRepo.findActiveByDriverId(driverId, List.of(LiveRideState.LIVE, LiveRideState.CONFIRMED, LiveRideState.VERIFIED));
        for (LiveRide r : existing) {
            r.setState(LiveRideState.CANCELLED);
            r.setCancelledAt(LocalDateTime.now());
            liveRideRepo.save(r);
            redisLocationCacheService.removeLiveRideLocation(r.getId());
        }

        BigDecimal extraKm = req.getExtraDistanceKm() != null ? req.getExtraDistanceKm() : BigDecimal.valueOf(3.0);
        double fullDist = GeoUtil.distanceKm(
                req.getFromLat().doubleValue(), req.getFromLng().doubleValue(),
                req.getToLat().doubleValue(), req.getToLng().doubleValue());

        LiveRide ride = LiveRide.builder()
                .driver(driver)
                .vehicle(vehicle)
                .fromName(req.getFromName())
                .fromLat(req.getFromLat())
                .fromLng(req.getFromLng())
                .toName(req.getToName())
                .toLat(req.getToLat())
                .toLng(req.getToLng())
                .distanceKm(BigDecimal.valueOf(fullDist).setScale(2, RoundingMode.HALF_UP))
                .extraDistanceKm(extraKm)
                .state(LiveRideState.LIVE)
                .build();

        ride = liveRideRepo.save(ride);
        cacheService.registerLiveDriver(driverId, ride.getId(), req);

        // Save initial driver location directly to Redis cache key live:live_ride:{rideId}
        redisLocationCacheService.saveLiveRideLocation(
                ride.getId(), driverId,
                req.getFromLat().doubleValue(), req.getFromLng().doubleValue(),
                0.0, 0.0, System.currentTimeMillis(), "LIVE");

        log.info("Driver id={} went LIVE. liveRideId={}", driverId, ride.getId());
        return LiveRideResponse.from(ride);
    }

    @Transactional
    public void stopLive(Long driverId) {
        List<LiveRide> active = liveRideRepo.findActiveByDriverId(driverId, List.of(LiveRideState.LIVE));
        for (LiveRide r : active) {
            r.setState(LiveRideState.CANCELLED);
            r.setCancelledAt(LocalDateTime.now());
            liveRideRepo.save(r);
        }
        cacheService.removeLiveDriver(driverId);
        log.info("Driver id={} stopped LIVE mode.", driverId);
    }

    public LiveRidePreviewResponse previewFare(LiveRidePreviewRequest req) {
        double dist = GeoUtil.distanceKm(
                req.getPickupLat().doubleValue(), req.getPickupLng().doubleValue(),
                req.getDropLat().doubleValue(), req.getDropLng().doubleValue());

        BigDecimal distanceKm = BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fare = PER_KM_RATE.multiply(distanceKm).setScale(2, RoundingMode.HALF_UP);
        if (fare.compareTo(MIN_FARE) < 0) {
            fare = MIN_FARE;
        }

        return LiveRidePreviewResponse.builder()
                .pickupName(req.getPickupName())
                .dropName(req.getDropName())
                .distanceKm(distanceKm)
                .estimatedFare(fare)
                .build();
    }

    public Long startSearch(Long bookerId, LiveRideSearchRequest req) {
        User booker = userRepo.findById(bookerId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        double dist = GeoUtil.distanceKm(
                req.getPickupLat().doubleValue(), req.getPickupLng().doubleValue(),
                req.getDropLat().doubleValue(), req.getDropLng().doubleValue());

        BigDecimal distanceKm = BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fare = PER_KM_RATE.multiply(distanceKm).setScale(2, RoundingMode.HALF_UP);
        if (fare.compareTo(MIN_FARE) < 0) {
            fare = MIN_FARE;
        }

        var searchReq = cacheService.registerSearchRequest(bookerId, req, distanceKm, fare);

        // Find matching live drivers within 2km radius or along route corridor
        var matchingDrivers = cacheService.findMatchingLiveDrivers(
                req.getPickupLat().doubleValue(), req.getPickupLng().doubleValue(),
                req.getDropLat().doubleValue(), req.getDropLng().doubleValue(),
                bookerId
        );

        if (matchingDrivers.isEmpty()) {
            log.info("No matching live drivers found for bookerId={}", bookerId);
        } else {
            for (var driverSession : matchingDrivers) {
                fcmService.sendToUser(
                        driverSession.getDriverId(),
                        "New Live Ride Request nearby",
                        booker.getFullName() + " requested a ride from " + req.getPickupName() + " to " + req.getDropName() + " (₹" + fare + ").",
                        Map.of(
                                "type", "LIVE_SEARCH_MATCH",
                                "searchRequestId", String.valueOf(searchReq.getRequestId()),
                                "bookerId", String.valueOf(bookerId),
                                "pickupName", req.getPickupName(),
                                "dropName", req.getDropName(),
                                "fare", fare.toString(),
                                "distanceKm", distanceKm.toString()
                        )
                );
                log.info("FCM live ride match notification sent to driverId={} for searchRequestId={}",
                        driverSession.getDriverId(), searchReq.getRequestId());
            }
        }

        return searchReq.getRequestId();
    }

    @Transactional
    public LiveRideResponse acceptRide(Long driverId, LiveRideAcceptRequest req) {
        // Atomically claim search request to prevent concurrent drivers from accepting the same request
        var searchReq = cacheService.claimSearchRequest(req.getSearchRequestId());
        if (searchReq == null) {
            throw AppException.notFound("Live ride search request expired or already accepted by another driver.");
        }

        User driver = userRepo.findById(driverId)
                .orElseThrow(() -> AppException.notFound("Driver not found"));
        User booker = userRepo.findById(searchReq.getBookerId())
                .orElseThrow(() -> AppException.notFound("Booker not found"));

        // Get active live ride for driver
        List<LiveRide> activeRides = liveRideRepo.findActiveByDriverId(driverId, List.of(LiveRideState.LIVE));
        LiveRide ride;
        if (!activeRides.isEmpty()) {
            ride = activeRides.get(0);
        } else {
            Vehicle vehicle = vehicleRepo.findByUserIdAndActiveTrue(driverId).orElse(null);
            ride = LiveRide.builder()
                    .driver(driver)
                    .vehicle(vehicle)
                    .fromName(searchReq.getPickupName())
                    .fromLat(BigDecimal.valueOf(searchReq.getPickupLat()))
                    .fromLng(BigDecimal.valueOf(searchReq.getPickupLng()))
                    .toName(searchReq.getDropName())
                    .toLat(BigDecimal.valueOf(searchReq.getDropLat()))
                    .toLng(BigDecimal.valueOf(searchReq.getDropLng()))
                    .build();
        }

        String otp = String.format("%04d", new SecureRandom().nextInt(10_000));

        ride.setBooker(booker);
        ride.setPickupName(searchReq.getPickupName());
        ride.setPickupLat(BigDecimal.valueOf(searchReq.getPickupLat()));
        ride.setPickupLng(BigDecimal.valueOf(searchReq.getPickupLng()));
        ride.setDropName(searchReq.getDropName());
        ride.setDropLat(BigDecimal.valueOf(searchReq.getDropLat()));
        ride.setDropLng(BigDecimal.valueOf(searchReq.getDropLng()));
        ride.setDistanceKm(searchReq.getDistanceKm());
        ride.setFare(searchReq.getFare());
        ride.setState(LiveRideState.CONFIRMED);
        ride.setBookerOtp(otp);
        ride.setStartedAt(LocalDateTime.now());

        ride = liveRideRepo.save(ride);

        // Notify Booker
        fcmService.sendToUser(
                booker.getId(),
                "Ride Confirmed!",
                driver.getFullName() + " accepted your ride request. OTP: " + otp,
                Map.of(
                        "type", "LIVE_RIDE_CONFIRMED",
                        "liveRideId", String.valueOf(ride.getId()),
                        "bookerOtp", otp,
                        "driverName", driver.getFullName(),
                        "driverPhone", driver.getPhone() != null ? driver.getPhone() : ""
                )
        );

        log.info("Live ride accepted: liveRideId={} driverId={} bookerId={} otp={}",
                ride.getId(), driverId, booker.getId(), otp);

        return LiveRideResponse.from(ride);
    }

    @Transactional
    public LiveRideResponse verifyOtp(Long driverId, Long liveRideId, String otp) {
        LiveRide ride = liveRideRepo.findByIdWithDetails(liveRideId)
                .orElseThrow(() -> AppException.notFound("Live ride not found"));

        if (!ride.getDriver().getId().equals(driverId)) {
            throw AppException.forbidden("Only the driver can verify OTP.");
        }
        if (ride.getState() != LiveRideState.CONFIRMED) {
            throw AppException.conflict("Live ride is not in CONFIRMED state. Current state: " + ride.getState());
        }
        if (!ride.getBookerOtp().equals(otp)) {
            throw AppException.badRequest("Invalid OTP code.");
        }

        ride.setState(LiveRideState.VERIFIED);
        ride.setVerifiedAt(LocalDateTime.now());
        ride = liveRideRepo.save(ride);

        if (ride.getBooker() != null) {
            fcmService.sendToUser(
                    ride.getBooker().getId(),
                    "Ride Verified",
                    "Your OTP has been verified. Have a safe journey!",
                    Map.of("type", "LIVE_RIDE_VERIFIED", "liveRideId", String.valueOf(liveRideId))
            );
        }

        log.info("Live ride OTP verified: liveRideId={} driverId={}", liveRideId, driverId);
        return LiveRideResponse.from(ride);
    }

    @Transactional
    public LiveRideResponse completeRide(Long driverId, Long liveRideId) {
        LiveRide ride = liveRideRepo.findByIdWithDetails(liveRideId)
                .orElseThrow(() -> AppException.notFound("Live ride not found"));

        if (!ride.getDriver().getId().equals(driverId)) {
            throw AppException.forbidden("Only the driver can complete the ride.");
        }

        ride.setState(LiveRideState.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride = liveRideRepo.save(ride);

        locationCacheService.removeLocation(liveRideId);
        cacheService.removeLiveDriver(driverId);

        String driverName = ride.getDriver().getFullName();
        if (ride.getBooker() != null) {
            fcmService.sendToUser(
                    ride.getBooker().getId(),
                    "Ride Completed",
                    "Your live ride with " + driverName + " has been completed safely.",
                    Map.of("type", "LIVE_RIDE_COMPLETED", "liveRideId", String.valueOf(liveRideId))
            );
        }

        log.info("Live ride completed: liveRideId={} driverId={}", liveRideId, driverId);
        return LiveRideResponse.from(ride);
    }

    public LiveRideResponse getMyActiveRide(Long userId) {
        List<LiveRide> active = liveRideRepo.findActiveByUserId(userId, List.of(LiveRideState.LIVE, LiveRideState.CONFIRMED, LiveRideState.VERIFIED));
        if (active.isEmpty()) {
            return null;
        }
        return LiveRideResponse.from(active.get(0));
    }
}
