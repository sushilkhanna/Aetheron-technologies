package com.bikepooling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PostRideRequestRequest {

    @NotBlank @Size(max = 255)
    private String pickupName;

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")
    private BigDecimal pickupLat;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal pickupLng;

    @NotBlank @Size(max = 255)
    private String dropName;

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")
    private BigDecimal dropLat;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal dropLng;

    @NotNull @Future
    private LocalDateTime departFrom;

    @NotNull @Future
    private LocalDateTime departTo;
}