package com.bikepooling.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
public class AdminRideStatsDTO {

    // KPI cards
    private long totalToday;
    private long completedToday;
    private long cancelledToday;
    private long expiredToday;
    private long activeToday;      // OPEN + BOOKED + STARTED + VERIFIED
    private long liveToday;
    private BigDecimal revenueToday;
    private int successRatePct;    // completed / (total - open - active) * 100

    // Graph data — last 7 days
    private List<DailyRideStat> daily;

    @Data
    @Builder
    public static class DailyRideStat {
        private String     date;       // "Mon", "Tue" etc
        private long       completed;
        private long       cancelled;
        private long       expired;
        private BigDecimal earning;
    }
}
