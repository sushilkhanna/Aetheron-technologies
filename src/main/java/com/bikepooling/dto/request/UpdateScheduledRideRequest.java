package com.bikepooling.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/** All fields optional — only supplied ones are updated. Location is never editable. */
@Getter
@Setter
@NoArgsConstructor
public class UpdateScheduledRideRequest {

    private Set<DayOfWeek> days;
    private LocalTime departTime;

    @DecimalMin("0.0") @DecimalMax("50.0")
    private BigDecimal extraDistanceKm;
}