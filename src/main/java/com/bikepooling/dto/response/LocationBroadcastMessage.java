package com.bikepooling.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationBroadcastMessage {
    private double lat;
    private double lng;
    private double bearingDegrees;
    private double speedKmh;
    private long   timestamp;
    private Long   rideId;
    private String driverName;
}