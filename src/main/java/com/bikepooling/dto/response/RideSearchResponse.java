package com.bikepooling.dto.response;

import com.bikepooling.entity.Ride;
import com.bikepooling.enums.PaymentMode;
import com.bikepooling.enums.PreferredGender;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Booker-facing search result. Deliberately excludes vehicle number, ride
 * state, and the driver's full-route reference fare -- none of those are
 * useful or safe to expose pre-booking. estimatedFare is computed for THIS
 * booker's actual pickup-to-drop, not the driver's whole-route fare.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideSearchResponse {

    private Long          id;
    private String        fromName;
    private String        toName;
    private LocalDateTime departAt;
    private BigDecimal    estimatedFare;
    private PaymentMode     paymentMode;
    private PreferredGender preferredGender;
    private String          routeNotes;
    private String          driverName;

    public static RideSearchResponse from(Ride ride, BigDecimal estimatedFare) {
        return RideSearchResponse.builder()
                .id(ride.getId())
                .fromName(ride.getFromName())
                .toName(ride.getToName())
                .departAt(ride.getDepartAt())
                .estimatedFare(estimatedFare)
                .paymentMode(ride.getPaymentMode())
                .preferredGender(ride.getPreferredGender())
                .routeNotes(ride.getRouteNotes())
                .driverName(firstNameOnly(ride.getPostedBy().getFullName()))
                .build();
    }

    private static String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Driver";
        return fullName.trim().split("\\s+")[0];
    }
}