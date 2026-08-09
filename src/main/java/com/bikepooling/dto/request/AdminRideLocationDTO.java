package com.bikepooling.dto.request;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class AdminRideLocationDTO {
    private Long   rideId;
    private double currentLat;
    private double currentLng;
    private double bookerDropLat;
    private double bookerDropLng;
    private boolean bookerDropSet;
    private long   lastUpdatedAt;
    // ride endpoints for drawing the route on map
    private double fromLat;
    private double fromLng;
    private double toLat;
    private double toLng;
}
