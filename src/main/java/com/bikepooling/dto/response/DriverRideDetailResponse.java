package com.bikepooling.dto.response;

import com.bikepooling.entity.Ride;
import com.bikepooling.entity.RideApplication;
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
public class DriverRideDetailResponse {

    private Long            id;
    private String          fromName;
    private String          toName;
    private LocalDateTime   departAt;

    /** Driver's base fare for their full route — internal reference only, never shown. */
    private BigDecimal      referenceFare;

    private BigDecimal      distanceKm;
    private BigDecimal      extraDistanceKm;

    private PreferredGender preferredGender;
    private PaymentMode     paymentMode;
    private String          routeNotes;
    private String          vehicleNumber;
    private String          state;

    private String          driverName;
    private String          driverPhone;

    private LocalDateTime   bookedAt;
    private LocalDateTime   startedAt;
    private LocalDateTime   completedAt;
    private LocalDateTime   cancelledAt;

    private BookerDetail    booker;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookerDetail {
        private String     name;
        private String     pickupName;
        private BigDecimal pickupLat;
        private BigDecimal pickupLng;
        private String     dropName;
        private BigDecimal dropLat;
        private BigDecimal dropLng;
        private BigDecimal bookerDistanceKm;
        private BigDecimal bookerFare;
        private String     note;
    }

    private static BookerDetail buildBookerDetail(RideApplication confirmedApp) {
        if (confirmedApp == null) return null;
        return BookerDetail.builder()
                .name(confirmedApp.getBooker().getFullName())
                .pickupName(confirmedApp.getPickupName())
                .pickupLat(confirmedApp.getPickupLat())
                .pickupLng(confirmedApp.getPickupLng())
                .dropName(confirmedApp.getDropName())
                .dropLat(confirmedApp.getDropLat())
                .dropLng(confirmedApp.getDropLng())
                .bookerDistanceKm(confirmedApp.getBookerDistanceKm())
                .bookerFare(confirmedApp.getBookerFare())
                .note(confirmedApp.getNote())
                .build();
    }

    public static DriverRideDetailResponse forDriver(Ride ride,
                                                     RideStatus status,
                                                     RideApplication confirmedApp) {
        return DriverRideDetailResponse.builder()
                .id(ride.getId())
                .fromName(ride.getFromName())
                .toName(ride.getToName())
                .departAt(ride.getDepartAt())
                .distanceKm(ride.getDistanceKm())
                .extraDistanceKm(ride.getExtraDistanceKm())
                .preferredGender(ride.getPreferredGender())
                .paymentMode(ride.getPaymentMode())
                .routeNotes(ride.getRouteNotes())
                .vehicleNumber(ride.getVehicle().getVehicleNumber())
                .state(status.getState().name())
                .bookedAt(status.getBookedAt())
                .startedAt(status.getStartedAt())
                .completedAt(status.getCompletedAt())
                .cancelledAt(status.getCancelledAt())
                .booker(buildBookerDetail(confirmedApp))
                .build();
    }

    public static DriverRideDetailResponse forBooker(Ride ride,
                                                     RideStatus status,
                                                     RideApplication confirmedApp) {
        return DriverRideDetailResponse.builder()
                .departAt(ride.getDepartAt())
                .distanceKm(ride.getDistanceKm())
                .paymentMode(ride.getPaymentMode())
                .routeNotes(ride.getRouteNotes())
                .vehicleNumber(ride.getVehicle().getVehicleNumber())
                .state(status.getState().name())
                .driverName(ride.getPostedBy().getFullName())
                .bookedAt(status.getBookedAt())
                .startedAt(status.getStartedAt())
                .completedAt(status.getCompletedAt())
                .cancelledAt(status.getCancelledAt())
                .booker(buildBookerDetail(confirmedApp))
                .build();
    }

    public static DriverRideDetailResponse from(Ride ride,
                                                RideStatus status,
                                                RideApplication confirmedApp) {
        return DriverRideDetailResponse.builder()
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
                .vehicleNumber(ride.getVehicle().getVehicleNumber())
                .state(status.getState().name())
                .driverName(ride.getPostedBy().getFullName())
                .driverPhone(ride.getPostedBy().getPhone())
                .bookedAt(status.getBookedAt())
                .startedAt(status.getStartedAt())
                .completedAt(status.getCompletedAt())
                .cancelledAt(status.getCancelledAt())
                .booker(buildBookerDetail(confirmedApp))
                .build();
    }
}