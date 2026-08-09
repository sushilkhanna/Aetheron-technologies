package com.bikepooling.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body for POST /api/rides/{rideId}/live
 * The driver's current GPS position at the moment they tap "Go Live".
 */
@Data
public class GoLiveRequest {

    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double lat;

    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double lng;
}