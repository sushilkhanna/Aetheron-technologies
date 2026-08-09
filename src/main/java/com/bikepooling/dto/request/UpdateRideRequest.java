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
public class UpdateRideRequest {

    private PaymentMode paymentMode;

    private PreferredGender preferredGender;

    @Size(max = 500, message = "Route notes too long")
    private String routeNotes;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "50.0", message = "Extra distance cannot exceed 50 km")
    private BigDecimal extraDistanceKm;
}