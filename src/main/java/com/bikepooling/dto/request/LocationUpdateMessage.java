package com.bikepooling.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationUpdateMessage {
    private double lat;
    private double lng;
    private double bearingDegrees;
    private double speedKmh;
    private long   timestamp;
}