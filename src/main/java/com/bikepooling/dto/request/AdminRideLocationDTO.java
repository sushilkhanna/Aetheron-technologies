package com.bikepooling.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRideLocationDTO {
    private Long id;
    private Long rideId;

    private double lat;
    private double lng;
    private double currentLat;
    private double currentLng;

    private double bookerDropLat;
    private double bookerDropLng;
    private boolean bookerDropSet;
    private long lastUpdatedAt;

    // ride endpoints for drawing the route on map
    private double fromLat;
    private double fromLng;
    private double toLat;
    private double toLng;
}
