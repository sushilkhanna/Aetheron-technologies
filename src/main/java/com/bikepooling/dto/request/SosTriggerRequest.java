package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SosTriggerRequest {

    @NotNull(message = "rideId is required")
    private Long rideId;

    @NotNull(message = "latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "longitude is required")
    private BigDecimal longitude;
}