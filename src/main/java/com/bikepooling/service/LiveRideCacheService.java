package com.bikepooling.service;

import com.bikepooling.dto.request.GoLiveRequest;
import com.bikepooling.dto.request.LiveRideSearchRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LiveRideCacheService {

    @Getter
    @Setter
    @Builder
    public static class LiveDriverSession {
        private Long driverId;
        private Long liveRideId;
        private String fromName;
        private double fromLat;
        private double fromLng;
        private String toName;
        private double toLat;
        private double toLng;
        private double currentLat;
        private double currentLng;
        private Double bearingDegrees;
        private Double speedKmh;
        private double extraDistanceKm;
        private long lastLocationTime;
    }

    @Getter
    @Setter
    @Builder
    public static class ActiveSearchRequest {
        private Long requestId;
        private Long bookerId;
        private String pickupName;
        private double pickupLat;
        private double pickupLng;
        private String dropName;
        private double dropLat;
        private double dropLng;
        private BigDecimal distanceKm;
        private BigDecimal fare;
        private String note;
        private long createdAt;
    }

    private final Map<Long, LiveDriverSession> liveDrivers = new ConcurrentHashMap<>();
    private final Map<Long, ActiveSearchRequest> searchRequests = new ConcurrentHashMap<>();
    private final AtomicLong searchIdCounter = new AtomicLong(1000);

    public void registerLiveDriver(Long driverId, Long liveRideId, GoLiveRequest req) {
        LiveDriverSession session = LiveDriverSession.builder()
                .driverId(driverId)
                .liveRideId(liveRideId)
                .fromName(req.getFromName())
                .fromLat(req.getFromLat().doubleValue())
                .fromLng(req.getFromLng().doubleValue())
                .toName(req.getToName())
                .toLat(req.getToLat().doubleValue())
                .toLng(req.getToLng().doubleValue())
                .currentLat(req.getFromLat().doubleValue())
                .currentLng(req.getFromLng().doubleValue())
                .extraDistanceKm(req.getExtraDistanceKm() != null ? req.getExtraDistanceKm().doubleValue() : 3.0)
                .lastLocationTime(System.currentTimeMillis())
                .build();

        liveDrivers.put(driverId, session);
    }

    public void updateDriverLocation(Long driverId, double lat, double lng, Double bearing, Double speed, long timestamp) {
        LiveDriverSession session = liveDrivers.get(driverId);
        if (session != null) {
            session.setCurrentLat(lat);
            session.setCurrentLng(lng);
            session.setBearingDegrees(bearing);
            session.setSpeedKmh(speed);
            session.setLastLocationTime(timestamp > 0 ? timestamp : System.currentTimeMillis());
        }
    }

    public LiveDriverSession getLiveDriver(Long driverId) {
        return liveDrivers.get(driverId);
    }

    public void removeLiveDriver(Long driverId) {
        liveDrivers.remove(driverId);
    }

    public Map<Long, LiveDriverSession> getAllLiveDrivers() {
        return liveDrivers;
    }

    public ActiveSearchRequest registerSearchRequest(Long bookerId, LiveRideSearchRequest req, BigDecimal distanceKm, BigDecimal fare) {
        long reqId = searchIdCounter.incrementAndGet();
        ActiveSearchRequest searchReq = ActiveSearchRequest.builder()
                .requestId(reqId)
                .bookerId(bookerId)
                .pickupName(req.getPickupName())
                .pickupLat(req.getPickupLat().doubleValue())
                .pickupLng(req.getPickupLng().doubleValue())
                .dropName(req.getDropName())
                .dropLat(req.getDropLat().doubleValue())
                .dropLng(req.getDropLng().doubleValue())
                .distanceKm(distanceKm)
                .fare(fare)
                .note(req.getNote())
                .createdAt(System.currentTimeMillis())
                .build();

        searchRequests.put(reqId, searchReq);
        return searchReq;
    }

    public ActiveSearchRequest getSearchRequest(Long requestId) {
        return searchRequests.get(requestId);
    }

    public void removeSearchRequest(Long requestId) {
        searchRequests.remove(requestId);
    }

    public List<LiveDriverSession> findMatchingLiveDrivers(double pickupLat, double pickupLng, double dropLat, double dropLng, Long excludeBookerId) {
        List<LiveDriverSession> matches = new ArrayList<>();
        for (LiveDriverSession driver : liveDrivers.values()) {
            if (driver.getDriverId().equals(excludeBookerId)) continue;

            // 1. Check if pickup point is within 2km radius of driver's current position OR along driver's route
            double distCurrentToPickup = com.bikepooling.util.GeoUtil.distanceKm(
                    driver.getCurrentLat(), driver.getCurrentLng(), pickupLat, pickupLng);

            var projPickup = com.bikepooling.util.GeoUtil.projectOntoRoute(
                    driver.getFromLat(), driver.getFromLng(), driver.getToLat(), driver.getToLng(), pickupLat, pickupLng);

            boolean pickupMatches = distCurrentToPickup <= 2.0 || projPickup.getOffRouteKm() <= 2.0;

            if (pickupMatches) {
                // 2. Check if drop point is along driver's route corridor within extraDistanceKm
                var projDrop = com.bikepooling.util.GeoUtil.projectOntoRoute(
                        driver.getFromLat(), driver.getFromLng(), driver.getToLat(), driver.getToLng(), dropLat, dropLng);

                if (projDrop.getOffRouteKm() <= driver.getExtraDistanceKm()) {
                    matches.add(driver);
                }
            }
        }
        return matches;
    }
}
