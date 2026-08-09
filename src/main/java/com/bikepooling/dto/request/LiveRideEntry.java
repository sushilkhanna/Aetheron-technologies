package com.bikepooling.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveRideEntry {

    private Long   rideId;
    private Long   driverId;

    // ride's posted start/end
    private double fromLat;
    private double fromLng;
    private double toLat;
    private double toLng;

    // driver's actual go-live position (seeded from fromLat/fromLng)
    private double goLiveLat;
    private double goLiveLng;

    // driver's current live location — updated every ~3 sec
    private double currentLat;
    private double currentLng;

    // booker's pickup point — used to calculate remainingDurationMinutes at OTP verify
    private double bookerPickupLat;
    private double bookerPickupLng;

    // booker's drop point — set when ride is CONFIRMED (live flow) or STARTED (pre-posted)
    // used as the destination check point in VERIFIED state
    private double  bookerDropLat;
    private double  bookerDropLng;
    private boolean bookerDropSet;

    // ride metadata
    private double distanceKm;
    private double extraDistanceKm;
    private String preferredGender;
    private String paymentMode;

    // timing
    private long lastUpdatedAt;
    private long goLiveAt;
    private int  estimatedDurationMinutes;
    private int  remainingDurationMinutes;

    // ── destination dwell tracking ─────────────────────────────────────────────
    private long   currentDwellStartedAt;
    private long   cumulativeDwellMs;
    private double minDistToDestKm;
    private long   verifiedStartAt;

    /**
     * Current ride state stored in Redis — avoids a DB hit on every 3-second
     * location ping. Updated in LiveRideService whenever state transitions occur.
     *
     * Stored as String (RideState.name()) so Jackson serialises it cleanly.
     * Null on first ping → falls back to one DB load, then cached.
     */
    private String currentState;
}