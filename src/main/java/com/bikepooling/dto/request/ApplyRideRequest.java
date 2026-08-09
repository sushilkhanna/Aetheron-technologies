package com.bikepooling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ApplyRideRequest {

    @NotBlank(message = "Pickup location name is required")
    @Size(max = 255)
    private String pickupName;

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal pickupLat;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal pickupLng;

    @NotBlank(message = "Drop location name is required")
    @Size(max = 255)
    private String dropName;

    @NotNull(message = "Drop latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal dropLat;

    @NotNull(message = "Drop longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal dropLng;

    @Size(max = 500, message = "Note too long")
    private String note;
}