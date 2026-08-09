package com.bikepooling.dto.response;

import com.bikepooling.entity.RideApplication;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Returned to the booker immediately after they apply -- an echo of their own submission + computed fare. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideApplicationResponse {

    private Long       id;
    private Long       rideId;

    private String     pickupName;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;

    private String     dropName;
    private BigDecimal dropLat;
    private BigDecimal dropLng;

    private String     note;

    private BigDecimal bookerDistanceKm;
    private BigDecimal bookerFare;

    private String        status;
    private LocalDateTime createdAt;

    public static RideApplicationResponse from(RideApplication app) {
        return RideApplicationResponse.builder()
                .id(app.getId())
                .rideId(app.getRide().getId())
                .pickupName(app.getPickupName())
                .pickupLat(app.getPickupLat())
                .pickupLng(app.getPickupLng())
                .dropName(app.getDropName())
                .dropLat(app.getDropLat())
                .dropLng(app.getDropLng())
                .note(app.getNote())
                .bookerDistanceKm(app.getBookerDistanceKm())
                .bookerFare(app.getBookerFare())
                .status(app.getStatus().name())
                .createdAt(app.getCreatedAt())
                .build();
    }
}