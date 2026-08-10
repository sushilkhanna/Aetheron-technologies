package com.bikepooling.dto.response;

import com.bikepooling.entity.ScheduledRideTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledRideTemplateResponse {

    private Long id;
    private String fromName;
    private String toName;
    private LocalTime departTime;
    private Set<LocalDate> dates;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private BigDecimal distanceKm;
    private BigDecimal extraDistanceKm;
    private String paymentMode;
    private String preferredGender;
    private String routeNotes;
    private String vehicleNumber;
    private String status;

    public static ScheduledRideTemplateResponse from(ScheduledRideTemplate t) {
        return ScheduledRideTemplateResponse.builder()
                .id(t.getId())
                .fromName(t.getFromName())
                .toName(t.getToName())
                .departTime(t.getDepartTime())
                .dates(t.getDates())
                .weekStart(t.getWeekStart())
                .weekEnd(t.getWeekEnd())
                .distanceKm(t.getDistanceKm())
                .extraDistanceKm(t.getExtraDistanceKm())
                .paymentMode(t.getPaymentMode().name())
                .preferredGender(t.getPreferredGender().name())
                .routeNotes(t.getRouteNotes())
                .vehicleNumber(t.getVehicle() != null ? t.getVehicle().getVehicleNumber() : null)
                .status(t.getStatus().name())
                .build();
    }
}