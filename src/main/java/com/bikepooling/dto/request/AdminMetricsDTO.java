package com.bikepooling.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminMetricsDTO {
    private long totalUsers;
    private long activeRides;
    private long ridesToday;
    private BigDecimal revenueToday;
}
