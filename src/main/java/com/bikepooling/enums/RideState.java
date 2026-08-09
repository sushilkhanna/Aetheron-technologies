package com.bikepooling.enums;

public enum RideState {
    OPEN,       // posted, waiting for applicants
    LIVE,
    BOOKED,     // driver confirmed a booker
    STARTED,    // ride is in progress
    COMPLETED,  // ride finished
    CANCELLED,  // cancelled by driver or booker
    EXPIRED,     // departAt passed with no booking and no cancellation
    VERIFIED,
    SOS_TRIGGERED
}