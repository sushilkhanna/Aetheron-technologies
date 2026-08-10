package com.bikepooling.dto.response;

import com.bikepooling.entity.LiveRide;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveRideResponse {

    private Long liveRideId;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;

    private Long bookerId;
    private String bookerName;
    private String bookerPhone;

    private String fromName;
    private String toName;
    private String pickupName;
    private String dropName;

    private BigDecimal distanceKm;
    private BigDecimal fare;
    private String state;
    private String bookerOtp;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;

    public static LiveRideResponse from(LiveRide ride) {
        var driver = ride.getDriver();
        var booker = ride.getBooker();
        var vehicle = ride.getVehicle();

        return LiveRideResponse.builder()
                .liveRideId(ride.getId())
                .driverId(driver != null ? driver.getId() : null)
                .driverName(driver != null ? driver.getFullName() : null)
                .driverPhone(driver != null ? driver.getPhone() : null)
                .vehicleNumber(vehicle != null ? vehicle.getVehicleNumber() : null)
                .bookerId(booker != null ? booker.getId() : null)
                .bookerName(booker != null ? booker.getFullName() : null)
                .bookerPhone(booker != null ? booker.getPhone() : null)
                .fromName(ride.getFromName())
                .toName(ride.getToName())
                .pickupName(ride.getPickupName())
                .dropName(ride.getDropName())
                .distanceKm(ride.getDistanceKm())
                .fare(ride.getFare())
                .state(ride.getState().name())
                .bookerOtp(ride.getBookerOtp())
                .createdAt(ride.getCreatedAt())
                .startedAt(ride.getStartedAt())
                .verifiedAt(ride.getVerifiedAt())
                .completedAt(ride.getCompletedAt())
                .build();
    }
}
