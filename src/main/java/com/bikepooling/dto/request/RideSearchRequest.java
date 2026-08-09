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
public class RideSearchRequest {

    @NotBlank(message = "Source name is required")
    private String sourceName;

    @NotNull(message = "Source latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal sourceLat;

    @NotNull(message = "Source longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal sourceLng;

    @NotBlank(message = "Destination name is required")
    private String destinationName;

    @NotNull(message = "Destination latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal destinationLat;

    @NotNull(message = "Destination longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal destinationLng;

    @NotNull(message = "Window start time is required")
    @Future(message = "Window start must be in the future")
    private LocalDateTime windowFrom;

    @NotNull(message = "Window end time is required")
    private LocalDateTime windowTo;

    // whether to save an alert if no rides found
    private boolean saveAlertIfEmpty = false;
}