package com.bikepooling.dto.response;

import com.bikepooling.enums.SosStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SosActiveSummaryResponse {
    private Long alertId;
    private Long instanceId;
    private String triggeredByName;
    private String triggeredByRole;
    private SosStatus status;
    private LocalDateTime triggeredAt;
}