package com.bikepooling.dto.response;

import com.bikepooling.entity.ScheduledRideInstance;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One bookable day — this is what shows up in booker search results. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledRideInstanceResponse {

    private Long id;
    private Long templateId;
    private LocalDate rideDate;
    private DayOfWeek dayOfWeek;
    private LocalTime departTime;
    private String fromName;
    private String toName;
    private BigDecimal distanceKm;
    private BigDecimal referenceFare;
    private String paymentMode;
    private String driverName;
    private String vehicleNumber;
    private String state;

    private LocalDateTime bookedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    public static ScheduledRideInstanceResponse from(ScheduledRideInstance inst) {
        var t = inst.getTemplate();
        return ScheduledRideInstanceResponse.builder()
                .id(inst.getId())
                .templateId(t.getId())
                .rideDate(inst.getRideDate())
                .dayOfWeek(inst.getDayOfWeek())
                .departTime(inst.getDepartTime())
                .fromName(t.getFromName())
                .toName(t.getToName())
                .distanceKm(t.getDistanceKm())
                .referenceFare(t.getFare())
                .paymentMode(t.getPaymentMode().name())
                .driverName(t.getPostedBy().getFullName())
                .vehicleNumber(t.getVehicle().getVehicleNumber())
                .state(inst.getState().name())
                .bookedAt(inst.getBookedAt())
                .startedAt(inst.getStartedAt())
                .completedAt(inst.getCompletedAt())
                .cancelledAt(inst.getCancelledAt())
                .build();
    }
}