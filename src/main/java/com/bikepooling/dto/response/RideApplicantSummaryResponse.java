package com.bikepooling.dto.response;

import com.bikepooling.entity.RideApplication;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Driver-facing view of a pending applicant. Deliberately omits exact
 * pickup/drop coordinates and the booker's internal user id -- the driver
 * only needs location NAMES, fare, and note to decide whether to confirm.
 * confirm()/reject() are keyed by applicationId, so bookerId isn't needed
 * here either. Exact coordinates are only revealed post-confirmation via
 * DriverRideDetailResponse.BookerDetail.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideApplicantSummaryResponse {

    private Long          id;      // applicationId -- used for confirm/reject
    private Long          rideId;
    private String        bookerName;
    private String        pickupName;
    private String        dropName;
    private BigDecimal    bookerDistanceKm;
    private BigDecimal    bookerFare;
    private String        note;
    private String        status;
    private LocalDateTime createdAt;

    public static RideApplicantSummaryResponse from(RideApplication app) {
        return RideApplicantSummaryResponse.builder()
                .id(app.getId())
                .rideId(app.getRide().getId())
                .bookerName(app.getBooker().getFullName())
                .pickupName(app.getPickupName())
                .dropName(app.getDropName())
                .bookerDistanceKm(app.getBookerDistanceKm())
                .bookerFare(app.getBookerFare())
                .note(app.getNote())
                .status(app.getStatus().name())
                .createdAt(app.getCreatedAt())
                .build();
    }
}