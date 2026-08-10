package com.bikepooling.enums;

public enum LiveRideState {
    LIVE,         // Driver is live and searching/waiting
    CONFIRMED,    // Driver accepted a booker, awaiting pickup (Streaming location to Booker)
    VERIFIED,     // Driver verified OTP upon pickup (Location stream to Booker stops, cache continues)
    COMPLETED,    // Ride finished
    CANCELLED,    // Ride cancelled
    EXPIRED       // Live mode timed out
}
