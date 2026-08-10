package com.bikepooling.dto.request;

import com.bikepooling.entity.LiveRide;
import com.bikepooling.entity.ScheduledRideInstance;
import com.bikepooling.entity.ScheduledRideTemplate;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AdminRideDTO {
    private Long instanceId;
    private Long templateId;
    private String rideType; // "SCHEDULED" or "LIVE"
    private String driverName;
    private Long driverId;
    private String bookerName;
    private Long bookerId;
    private String state;
    private String fromName;
    private String toName;
    private LocalDate rideDate;
    private LocalTime departTime;
    private LocalDateTime startedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private BigDecimal distanceKm;
    private BigDecimal fare;

    public static AdminRideDTO from(ScheduledRideInstance inst) {
        ScheduledRideTemplate t = inst.getTemplate();
        return AdminRideDTO.builder()
                .instanceId(inst.getId())
                .templateId(t.getId())
                .rideType("SCHEDULED")
                .driverName(t.getPostedBy().getFullName())
                .driverId(t.getPostedBy().getId())
                .bookerName(inst.getBookedBy() != null ? inst.getBookedBy().getFullName() : null)
                .bookerId(inst.getBookedBy() != null ? inst.getBookedBy().getId() : null)
                .state(inst.getState().name())
                .fromName(t.getFromName())
                .toName(t.getToName())
                .rideDate(inst.getRideDate())
                .departTime(inst.getDepartTime())
                .startedAt(inst.getStartedAt())
                .verifiedAt(inst.getVerifiedAt())
                .completedAt(inst.getCompletedAt())
                .cancelledAt(inst.getCancelledAt())
                .distanceKm(t.getDistanceKm())
                .fare(t.getFare())
                .build();
    }

    public static AdminRideDTO from(LiveRide live) {
        return AdminRideDTO.builder()
                .instanceId(live.getId())
                .templateId(null)
                .rideType("LIVE")
                .driverName(live.getDriver() != null ? live.getDriver().getFullName() : null)
                .driverId(live.getDriver() != null ? live.getDriver().getId() : null)
                .bookerName(live.getBooker() != null ? live.getBooker().getFullName() : null)
                .bookerId(live.getBooker() != null ? live.getBooker().getId() : null)
                .state(live.getState().name())
                .fromName(live.getFromName())
                .toName(live.getToName())
                .rideDate(live.getCreatedAt() != null ? live.getCreatedAt().toLocalDate() : LocalDate.now())
                .departTime(live.getCreatedAt() != null ? live.getCreatedAt().toLocalTime() : LocalTime.now())
                .startedAt(live.getStartedAt())
                .verifiedAt(live.getVerifiedAt())
                .completedAt(live.getCompletedAt())
                .cancelledAt(live.getCancelledAt())
                .distanceKm(live.getDistanceKm())
                .fare(live.getFare())
                .build();
    }
}