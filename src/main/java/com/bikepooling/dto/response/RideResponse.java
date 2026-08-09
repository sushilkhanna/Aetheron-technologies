package com.bikepooling.dto.response;

import com.bikepooling.entity.Ride;
import com.bikepooling.entity.RideStatus;
import com.bikepooling.enums.PaymentMode;
import com.bikepooling.enums.PreferredGender;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideResponse {

    private Long            id;
    private String          fromName;
    private String          toName;
    private LocalDateTime   departAt;
    private BigDecimal      referenceFare;
    private BigDecimal      distanceKm;
    private BigDecimal      extraDistanceKm;
    private PreferredGender preferredGender;
    private PaymentMode     paymentMode;
    private String          routeNotes;
    private String          state;
    private String          driverName;
    private String          vehicleNumber;

    /** Present only when this ride has a paired return ride. */
    private Long            returnRideId;

    /** True when this ride is itself the return leg of another ride. */
    private Boolean         isReturnRide;

    public static RideResponse from(Ride ride, RideStatus status) {
        return RideResponse.builder()
                .id(ride.getId())
                .fromName(ride.getFromName())
                .toName(ride.getToName())
                .departAt(ride.getDepartAt())
                .referenceFare(ride.getFare())
                .distanceKm(ride.getDistanceKm())
                .extraDistanceKm(ride.getExtraDistanceKm())
                .preferredGender(ride.getPreferredGender())
                .paymentMode(ride.getPaymentMode())
                .routeNotes(ride.getRouteNotes())
                .state(status != null ? status.getState().name() : null)
                .driverName(firstNameOnly(ride.getPostedBy().getFullName()))
                .vehicleNumber(ride.getVehicle().getVehicleNumber())
                .returnRideId(ride.getReturnRideId())
                .isReturnRide(ride.isReturnRide() ? Boolean.TRUE : null)
                .build();
    }

    public static RideResponse forDriver(Ride ride, RideStatus status) {
        return RideResponse.builder()
                .id(ride.getId())
                .fromName(ride.getFromName())
                .toName(ride.getToName())
                .departAt(ride.getDepartAt())
                .referenceFare(ride.getFare())
                .distanceKm(ride.getDistanceKm())
                .extraDistanceKm(ride.getExtraDistanceKm())
                .paymentMode(ride.getPaymentMode())
                .preferredGender(ride.getPreferredGender())
                .routeNotes(ride.getRouteNotes())
                .state(status != null ? status.getState().name() : null)
                .vehicleNumber(ride.getVehicle().getVehicleNumber())
                .returnRideId(ride.getReturnRideId())
                .isReturnRide(ride.isReturnRide() ? Boolean.TRUE : null)
                .build();
    }

    private static String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Driver";
        return fullName.trim().split("\\s+")[0];
    }
}