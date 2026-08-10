package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class LiveRidePreviewRequest {

    @NotBlank
    private String pickupName;

    @NotNull
    private BigDecimal pickupLat;

    @NotNull
    private BigDecimal pickupLng;

    @NotBlank
    private String dropName;

    @NotNull
    private BigDecimal dropLat;

    @NotNull
    private BigDecimal dropLng;
}
