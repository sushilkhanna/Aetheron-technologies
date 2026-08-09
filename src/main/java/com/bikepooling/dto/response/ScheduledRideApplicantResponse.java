package com.bikepooling.dto.response;

import com.bikepooling.entity.ScheduledRideApplication;
import com.bikepooling.entity.ScheduledRideApplicationDay;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledRideApplicantResponse {

    private Long applicationId;
    private Long bookerId;
    private String bookerName;
    private String bookerPhone;
    private String pickupName;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private String dropName;
    private BigDecimal dropLat;
    private BigDecimal dropLng;
    private String note;
    private BigDecimal bookerDistanceKm;
    private BigDecimal bookerFare;

    private List<ApplicationDayItem> appliedDays;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicationDayItem {
        private Long applicationDayId;
        private Long instanceId;
        private LocalDate rideDate;
        private String status;
    }

    public static ScheduledRideApplicantResponse from(ScheduledRideApplication app, List<ScheduledRideApplicationDay> days) {
        List<ApplicationDayItem> dayItems = days.stream()
                .map(d -> ApplicationDayItem.builder()
                        .applicationDayId(d.getId())
                        .instanceId(d.getInstance().getId())
                        .rideDate(d.getInstance().getRideDate())
                        .status(d.getStatus().name())
                        .build())
                .toList();

        return ScheduledRideApplicantResponse.builder()
                .applicationId(app.getId())
                .bookerId(app.getBooker().getId())
                .bookerName(app.getBooker().getFullName())
                .bookerPhone(app.getBooker().getPhone())
                .pickupName(app.getPickupName())
                .pickupLat(app.getPickupLat())
                .pickupLng(app.getPickupLng())
                .dropName(app.getDropName())
                .dropLat(app.getDropLat())
                .dropLng(app.getDropLng())
                .note(app.getNote())
                .bookerDistanceKm(app.getBookerDistanceKm())
                .bookerFare(app.getBookerFare())
                .appliedDays(dayItems)
                .build();
    }
}
