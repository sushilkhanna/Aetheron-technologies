package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateLaunchConfigRequest {

    @NotBlank(message = "Launch mode is required (COMING_SOON or LIVE_LAUNCHED)")
    private String launchMode; // COMING_SOON vs LIVE_LAUNCHED

    private LocalDateTime launchTargetDateTime;
    private String androidAppUrl;
    private String iosAppUrl;
    private String launchMessage;
}
