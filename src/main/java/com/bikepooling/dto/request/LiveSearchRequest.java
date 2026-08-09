package com.bikepooling.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Sent by the booker when they tap "Start Search".
 * The search window is fixed at 3 minutes server-side.
 */
@Data
public class LiveSearchRequest {

    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal pickupLat;

    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal pickupLng;

    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal dropLat;

    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal dropLng;

    private String pickupName;
    private String dropName;
}