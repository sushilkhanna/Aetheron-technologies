package com.bikepooling.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slow-changing ride/driver/route info for a live ride.
 * Written at goLive/seed time and only on booking/verify transitions —
 * NOT rewritten on every 3-second location tick (see LiveRideLocation for that).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveRideMeta {

    private Long   rideId;
    private Long   driverId;
    private String vehicleNumber;

    private double fromLat;
    private double fromLng;
    private double toLat;
    private double toLng;

    private double goLiveLat;
    private double goLiveLng;

    private double bookerPickupLat;
    private double bookerPickupLng;
    private double bookerDropLat;
    private double bookerDropLng;
    private boolean bookerDropSet;

    private double distanceKm;
    private double extraDistanceKm;
    private String preferredGender;
    private String paymentMode;

    private long goLiveAt;
    private int  estimatedDurationMinutes;
    private int  remainingDurationMinutes;
    private long verifiedStartAt;

    /** RideState.name() — cached so location pings never need a DB hit. */
    private String currentState;
}