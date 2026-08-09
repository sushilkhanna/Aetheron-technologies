package com.bikepooling.dto.response;

import com.bikepooling.enums.RideState;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduledRideLocationBroadcast {

    private Long      instanceId;
    private double    lat;
    private double    lng;
    private Double    bearingDegrees;
    private Double    speedKmh;
    private Long      timestamp;
    private String    driverName;
    private RideState state;
}
