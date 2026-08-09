package com.bikepooling.dto.response;

import com.bikepooling.entity.ScheduledRideInstance;
import com.bikepooling.entity.ScheduledRideTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledRideSearchResultItem {

    private Long templateId;
    private Long driverId;
    private String driverName;
    private String vehicleNumber;
    private String fromName;
    private String toName;
    private LocalTime departTime;
    private BigDecimal distanceKm;
    private BigDecimal referenceFare;
    private String paymentMode;
    private String preferredGender;
    private String routeNotes;

    /** All requested dates on which this driver/template is available and OPEN for booking. */
    private Set<LocalDate> availableDates;

    /** Matching instance details per date. */
    private List<InstanceItem> matchingInstances;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InstanceItem {
        private Long instanceId;
        private LocalDate rideDate;
        private DayOfWeek dayOfWeek;
        private LocalTime departTime;
        private String state;
    }

    public static ScheduledRideSearchResultItem from(ScheduledRideTemplate template, List<ScheduledRideInstance> instances) {
        Set<LocalDate> availableDates = instances.stream()
                .map(ScheduledRideInstance::getRideDate)
                .collect(Collectors.toCollection(TreeSet::new));

        List<InstanceItem> instanceItems = instances.stream()
                .map(inst -> InstanceItem.builder()
                        .instanceId(inst.getId())
                        .rideDate(inst.getRideDate())
                        .dayOfWeek(inst.getDayOfWeek())
                        .departTime(inst.getDepartTime())
                        .state(inst.getState().name())
                        .build())
                .toList();

        return ScheduledRideSearchResultItem.builder()
                .templateId(template.getId())
                .driverId(template.getPostedBy().getId())
                .driverName(template.getPostedBy().getFullName())
                .vehicleNumber(template.getVehicle() != null ? template.getVehicle().getVehicleNumber() : null)
                .fromName(template.getFromName())
                .toName(template.getToName())
                .departTime(template.getDepartTime())
                .distanceKm(template.getDistanceKm())
                .referenceFare(template.getFare())
                .paymentMode(template.getPaymentMode() != null ? template.getPaymentMode().name() : null)
                .preferredGender(template.getPreferredGender() != null ? template.getPreferredGender().name() : null)
                .routeNotes(template.getRouteNotes())
                .availableDates(availableDates)
                .matchingInstances(instanceItems)
                .build();
    }
}
