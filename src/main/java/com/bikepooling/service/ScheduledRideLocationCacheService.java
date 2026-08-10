package com.bikepooling.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScheduledRideLocationCacheService {

    @Getter
    @Setter
    @Builder
    public static class RideLocationSnapshot {
        private Long instanceId;
        private Long driverId;
        private double lat;
        private double lng;
        private Double bearingDegrees;
        private Double speedKmh;
        private long timestamp;

        // Auto-completion & safety monitoring state
        private Long nearDropPointStartTime;
        private Long offRouteStartTime;
        private Long passedDestinationStartTime;
        private boolean offRouteAlertSent;

        // Stationary / Idle monitoring state
        private Long stationaryStartTime;
        private Double stationaryLat;
        private Double stationaryLng;
    }

    private final Map<Long, RideLocationSnapshot> cache = new ConcurrentHashMap<>();

    public void updateLocation(Long instanceId, double lat, double lng, Double bearingDegrees, Double speedKmh, long timestamp, Long driverId) {
        cache.compute(instanceId, (id, existing) -> {
            long now = System.currentTimeMillis();
            long updateTs = timestamp > 0 ? timestamp : now;
            if (existing == null) {
                return RideLocationSnapshot.builder()
                        .instanceId(instanceId)
                        .driverId(driverId)
                        .lat(lat)
                        .lng(lng)
                        .bearingDegrees(bearingDegrees)
                        .speedKmh(speedKmh)
                        .timestamp(updateTs)
                        .stationaryLat(lat)
                        .stationaryLng(lng)
                        .stationaryStartTime(now)
                        .build();
            } else {
                double prevLat = existing.getLat();
                double prevLng = existing.getLng();
                double distKm = com.bikepooling.util.GeoUtil.distanceKm(prevLat, prevLng, lat, lng);

                if (distKm <= 0.05) { // Within 50 meters
                    if (existing.getStationaryStartTime() == null) {
                        existing.setStationaryStartTime(now);
                        existing.setStationaryLat(lat);
                        existing.setStationaryLng(lng);
                    }
                } else {
                    existing.setStationaryStartTime(now);
                    existing.setStationaryLat(lat);
                    existing.setStationaryLng(lng);
                }

                existing.setLat(lat);
                existing.setLng(lng);
                existing.setBearingDegrees(bearingDegrees);
                existing.setSpeedKmh(speedKmh);
                existing.setTimestamp(updateTs);
                if (driverId != null) {
                    existing.setDriverId(driverId);
                }
                return existing;
            }
        });
    }

    public RideLocationSnapshot getLocation(Long instanceId) {
        return cache.get(instanceId);
    }

    public void removeLocation(Long instanceId) {
        cache.remove(instanceId);
    }

    public Map<Long, RideLocationSnapshot> getAllSnapshots() {
        return cache;
    }
}
