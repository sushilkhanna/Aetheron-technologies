package com.bikepooling.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** Driver can update dates (only OPEN instances can be modified/removed) and optional extra distance. Time editing is not allowed. */
@Getter
@Setter
@NoArgsConstructor
public class UpdateScheduledRideRequest {

    private Set<LocalDate> dates;

    @DecimalMin("0.0") @DecimalMax("50.0")
    private BigDecimal extraDistanceKm;
}