package com.bikepooling.enums;

public enum ApplicationStatus {
    PENDING,       // booker applied, waiting for driver
    CONFIRMED,     // driver accepted this booker
    REJECTED,      // driver rejected this booker
    WITHDRAWN,     // booker cancelled their own application
    FINISH,
    EXPIRED        // ride expired before driver responded
}