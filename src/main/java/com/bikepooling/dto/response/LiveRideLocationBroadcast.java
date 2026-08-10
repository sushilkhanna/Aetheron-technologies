package com.bikepooling.dto.response;

import com.bikepooling.enums.LiveRideState;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveRideLocationBroadcast {

    private Long liveRideId;
    private double lat;
    private double lng;
    private Double bearingDegrees;
    private Double speedKmh;
    private long timestamp;
    private String driverName;
    private LiveRideState state;
}
