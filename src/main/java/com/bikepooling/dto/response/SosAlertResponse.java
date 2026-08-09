package com.bikepooling.dto.response;

import com.bikepooling.enums.SosStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SosAlertResponse {
    private Long id;
    private String trackingToken;
    private Long rideId;
    private Long triggeredByUserId;
    private String triggeredByName;
    private String triggeredByRole;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private SosStatus status;
    private boolean showedCall112Option;
    private boolean contactSmsSent;
    private boolean adminSmsSent;
    private boolean counterpartNotified;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;

    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private String fromName;
    private String toName;
}