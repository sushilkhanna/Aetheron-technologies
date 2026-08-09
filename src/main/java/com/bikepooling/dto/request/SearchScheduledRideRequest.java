package com.bikepooling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class SearchScheduledRideRequest {

    @NotBlank
    private String sourceName;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal sourceLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal sourceLng;

    @NotBlank
    private String destinationName;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal destinationLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal destinationLng;

    /** Single date search */
    private LocalDate date;

    /** Multi-date search */
    private Set<LocalDate> dates;

    /** Required only when `date` or `dates` is not supplied. */
    private Set<DayOfWeek> wantedDays;

    @NotNull
    private LocalTime windowFrom;

    @NotNull
    private LocalTime windowTo;
}