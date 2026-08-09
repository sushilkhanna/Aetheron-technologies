package com.bikepooling.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SosLocationPingResponse {
    private Long id;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime recordedAt;
}