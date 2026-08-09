package com.bikepooling.dto.request;

import com.bikepooling.entity.Ride;
import com.bikepooling.entity.RideStatus;
import com.bikepooling.enums.RideState;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AdminRideDTO.java
@Data
@Builder
public class AdminRideDTO {
    private Long        rideId;
    private String      driverName;
    private Long        driverId;
    private String      bookerName;
    private Long        bookerId;
    private String      state;
    private String      fromName;
    private String      toName;
    private LocalDateTime departAt;
    private LocalDateTime startedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private BigDecimal distanceKm;
    private BigDecimal  fare;
    private boolean     isLiveRide;

    public static AdminRideDTO from(RideStatus rs) {
        Ride r = rs.getRide();
        return AdminRideDTO.builder()
                .rideId(r.getId())
                .driverName(r.getPostedBy().getFullName())
                .driverId(r.getPostedBy().getId())
                .bookerName(rs.getBookedBy() != null
                        ? rs.getBookedBy().getFullName() : null)
                .bookerId(rs.getBookedBy() != null
                        ? rs.getBookedBy().getId() : null)
                .state(rs.getState().name())
                .fromName(r.getFromName())
                .toName(r.getToName())
                .departAt(r.getDepartAt())
                .startedAt(rs.getStartedAt())
                .verifiedAt(rs.getVerifiedAt())
                .completedAt(rs.getCompletedAt())
                .cancelledAt(rs.getCancelledAt())
                .distanceKm(r.getDistanceKm())
                .fare(r.getFare())
                .isLiveRide(rs.getState() == RideState.LIVE
                        || rs.getStartedAt() != null /* seeded from live flow */)
                .build();
    }
}