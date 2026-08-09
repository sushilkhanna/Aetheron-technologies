package com.bikepooling.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fast-changing location + dwell-detection state for a live ride.
 * Overwritten on every ~3-second GPS ping — kept separate from LiveRideMeta
 * so each tick only rewrites a small payload instead of the whole ride object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveRideLocation {

    private Long   rideId;
    private double currentLat;
    private double currentLng;
    private double bearingDegrees;
    private double speedKmh;
    private long   lastUpdatedAt;

    // dwell / pass-through tracking (LIVE destination or VERIFIED booker-drop)
    private long   currentDwellStartedAt;
    private long   cumulativeDwellMs;
    private double minDistToDestKm;
}