package com.bikepooling.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private String fromName;
    private String toName;
    private LocalTime departTime;
    private String pickupName;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private String dropName;
    private BigDecimal dropLat;
    private BigDecimal dropLng;
    private String note;
    private BigDecimal bookerDistanceKm;
    private BigDecimal bookerFare;

    private List<BookerApplicationDayItem> appliedDays;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookerApplicationDayItem {
        private Long applicationDayId;
        private Long instanceId;
        private LocalDate rideDate;
        private String status;      // PENDING, CONFIRMED, REJECTED, WITHDRAWN
        private String rideState;   // OPEN, BOOKED, STARTED, VERIFIED, COMPLETED, CANCELLED
        private String otp;         // Visible to Booker when CONFIRMED (in BOOKED/STARTED/VERIFIED states)
    }
}
