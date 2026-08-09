package com.bikepooling.dto.request;

import com.bikepooling.enums.PaymentMode;
import com.bikepooling.enums.PreferredGender;
import com.bikepooling.enums.ScheduleWeek;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class PostScheduledRideRequest {

    @NotBlank @Size(max = 255)
    private String fromName;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal fromLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal fromLng;

    @NotBlank @Size(max = 255)
    private String toName;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal toLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal toLng;

    @NotNull(message = "Departure time is required")
    private LocalTime departTime;

    /** Which week block this scheduled ride applies to -- CURRENT or NEXT, never a mix. */
    @NotNull(message = "Select whether this ride is for the current week or next week")
    private ScheduleWeek week;

    @NotEmpty(message = "Select at least one day")
    private Set<DayOfWeek> days;

    @NotNull
    private PaymentMode paymentMode;

    private PreferredGender preferredGender;

    @Size(max = 500)
    private String routeNotes;

    @NotNull
    @DecimalMin("0.0") @DecimalMax("50.0")
    private BigDecimal extraDistanceKm;
}