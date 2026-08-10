package com.bikepooling.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveRidePreviewResponse {

    private String pickupName;
    private String dropName;
    private BigDecimal distanceKm;
    private BigDecimal estimatedFare;
}
