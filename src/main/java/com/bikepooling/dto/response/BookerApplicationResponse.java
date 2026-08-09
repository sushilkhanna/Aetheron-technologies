package com.bikepooling.dto.response;

import com.bikepooling.entity.RideApplication;
import com.bikepooling.enums.RideState;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookerApplicationResponse {

    private Long          id;
    private Long          rideId;
    private String        fromName;
    private String        toName;
    private LocalDateTime departAt;
    private BigDecimal    referenceFare;
    private String        pickupName;
    private String        dropName;
    private BigDecimal    bookerDistanceKm;
    private BigDecimal    bookerFare;
    private String        status;

    private RideState     rideState;

    private LocalDateTime createdAt;

    private String        otp;

    public static BookerApplicationResponse from(RideApplication app) {
        return from(app, null, null);
    }

    public static BookerApplicationResponse from(RideApplication app, String otp) {
        return from(app, otp, null);
    }

    public static BookerApplicationResponse from(RideApplication app, String otp, RideState rideState) {
        return BookerApplicationResponse.builder()
                .id(app.getId())
                .rideId(app.getRide().getId())
                .fromName(app.getRide().getFromName())
                .toName(app.getRide().getToName())
                .departAt(app.getRide().getDepartAt())
                .referenceFare(app.getRide().getFare())
                .pickupName(app.getPickupName())
                .dropName(app.getDropName())
                .bookerDistanceKm(app.getBookerDistanceKm())
                .bookerFare(app.getBookerFare())
                .status(app.getStatus().name())
                .rideState(rideState)
                .createdAt(app.getCreatedAt())
                .otp(otp)
                .build();
    }
}