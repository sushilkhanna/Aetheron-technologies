package com.bikepooling.dto.response;

import com.bikepooling.entity.ScheduledRideApplication;
import com.bikepooling.entity.ScheduledRideApplicationDay;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledRideBookerApplicationResponse {

    private Long applicationId;
    private Long templateId;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private String pickupName;
    private String dropName;
    private BigDecimal bookerDistanceKm;
    private BigDecimal bookerFare;
    private String note;
    private LocalDateTime createdAt;
    private List<BookerDayItem> days;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookerDayItem {
        private Long applicationDayId;
        private Long instanceId;
        private LocalDate rideDate;
        private LocalTime departTime;
        private String applicationStatus;
        private String instanceState;
        private String bookerOtp;
    }

    public static ScheduledRideBookerApplicationResponse from(ScheduledRideApplication app, List<ScheduledRideApplicationDay> days) {
        List<BookerDayItem> dayItems = days.stream()
                .map(d -> BookerDayItem.builder()
                        .applicationDayId(d.getId())
                        .instanceId(d.getInstance().getId())
                        .rideDate(d.getInstance().getRideDate())
                        .departTime(d.getInstance().getDepartTime())
                        .applicationStatus(d.getStatus().name())
                        .instanceState(d.getInstance().getState().name())
                        .bookerOtp("CONFIRMED".equals(d.getStatus().name()) ? d.getInstance().getBookerOtp() : null)
                        .build())
                .toList();

        var template = app.getTemplate();
        var driver = template.getPostedBy();
        var vehicle = template.getVehicle();

        return ScheduledRideBookerApplicationResponse.builder()
                .applicationId(app.getId())
                .templateId(template.getId())
                .driverId(driver.getId())
                .driverName(driver.getFullName())
                .driverPhone(driver.getPhone())
                .vehicleNumber(vehicle != null ? vehicle.getVehicleNumber() : null)
                .pickupName(app.getPickupName())
                .dropName(app.getDropName())
                .bookerDistanceKm(app.getBookerDistanceKm())
                .bookerFare(app.getBookerFare())
                .note(app.getNote())
                .createdAt(app.getCreatedAt())
                .days(dayItems)
                .build();
    }
}
