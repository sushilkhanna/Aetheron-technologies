package com.bikepooling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaunchConfigDTO {
    private String launchMode; // COMING_SOON vs LIVE_LAUNCHED
    private LocalDateTime launchTargetDateTime;
    private String androidAppUrl;
    private String iosAppUrl;
    private String launchMessage;
    private LocalDateTime updatedAt;
}
