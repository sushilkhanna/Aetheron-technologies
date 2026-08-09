package com.bikepooling.dto.request;

import com.bikepooling.enums.ExtraDistanceType;
import com.bikepooling.enums.PaymentMode;
import com.bikepooling.enums.PreferredGender;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PostRideRequest {

    @NotBlank(message = "Source name is required")
    @Size(max = 255)
    private String fromName;

    @NotNull(message = "Source latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal fromLat;

    @NotNull(message = "Source longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal fromLng;

    @NotBlank(message = "Destination name is required")
    @Size(max = 255)
    private String toName;

    @NotNull(message = "Destination latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal toLat;

    @NotNull(message = "Destination longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal toLng;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    private LocalDateTime departAt;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private PreferredGender preferredGender;

    @Size(max = 500)
    private String routeNotes;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "50.0", message = "Extra distance cannot exceed 50 km")
    @NotNull(message = "Extra Distance is required for better matching")
    private BigDecimal extraDistanceKm;

    private boolean wantReturnRide = false;

    @Future(message = "Return departure time must be in the future")
    private LocalDateTime returnDepartAt;
}