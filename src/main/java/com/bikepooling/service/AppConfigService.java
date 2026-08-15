package com.bikepooling.service;

import com.bikepooling.dto.request.UpdateLaunchConfigRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.LaunchConfigDTO;
import com.bikepooling.entity.AppConfig;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConfigService {

    private static final Long CONFIG_ID = 1L;
    private final AppConfigRepository configRepo;

    @Transactional
    public AppConfig get() {
        return configRepo.findById(CONFIG_ID)
                .orElseGet(() -> {
                    log.info("AppConfig record #1 not found. Seeding default row...");
                    AppConfig defaultConfig = AppConfig.builder()
                            .id(CONFIG_ID)
                            .farePerKm(new BigDecimal("3.00"))
                            .minFare(new BigDecimal("10.00"))
                            .matchRadiusMetres(500)
                            .matchTimeWindowMinutes(30)
                            .launchMode("COMING_SOON")
                            .launchMessage("We are launching soon across India! Stay tuned.")
                            .build();
                    return configRepo.save(defaultConfig);
                });
    }

    @Transactional
    public LaunchConfigDTO getLaunchConfig() {
        AppConfig config = get();

        // Auto-switch to LIVE_LAUNCHED if COMING_SOON target date time has been reached
        if ("COMING_SOON".equalsIgnoreCase(config.getLaunchMode())
                && config.getLaunchTargetDateTime() != null
                && !LocalDateTime.now().isBefore(config.getLaunchTargetDateTime())) {
            log.info("Launch target time reached ({}). Auto-switching launchMode to LIVE_LAUNCHED.", config.getLaunchTargetDateTime());
            config.setLaunchMode("LIVE_LAUNCHED");
            config = configRepo.save(config);
        }

        return toLaunchDTO(config);
    }

    @Transactional
    public ApiResponse<LaunchConfigDTO> updateLaunchConfig(UpdateLaunchConfigRequest request) {
        AppConfig config = get();

        String rawMode = request.getLaunchMode() != null ? request.getLaunchMode().trim() : "COMING_SOON";
        String mode = ("LIVE_LAUNCHED".equalsIgnoreCase(rawMode) || "LIVE".equalsIgnoreCase(rawMode))
                ? "LIVE_LAUNCHED"
                : "COMING_SOON";

        LocalDateTime targetDateTime = request.getLaunchTargetDateTime();
        String androidUrl = request.getAndroidAppUrl() != null ? request.getAndroidAppUrl().trim() : "";
        String iosUrl = request.getIosAppUrl() != null ? request.getIosAppUrl().trim() : "";

        // Restriction rules:
        // 1. If mode is LIVE_LAUNCHED -> Android & iOS links ARE REQUIRED
        // 2. If mode is COMING_SOON and launchTargetDateTime is SET -> Android & iOS links ARE REQUIRED
        // 3. If mode is COMING_SOON and launchTargetDateTime is NULL/EMPTY -> Links are NOT required
        boolean requiresLinks = "LIVE_LAUNCHED".equals(mode) || (targetDateTime != null);

        if (requiresLinks) {
            if (androidUrl.isEmpty()) {
                throw AppException.badRequest("Android Play Store link is required when setting mode to Live or fixing a target Launch Time.");
            }
            if (iosUrl.isEmpty()) {
                throw AppException.badRequest("iOS App Store link is required when setting mode to Live or fixing a target Launch Time.");
            }
        }

        // Auto-promote to LIVE_LAUNCHED if setting COMING_SOON with a target time that is already past
        if ("COMING_SOON".equals(mode) && targetDateTime != null && !LocalDateTime.now().isBefore(targetDateTime)) {
            mode = "LIVE_LAUNCHED";
        }

        config.setLaunchMode(mode);
        config.setLaunchTargetDateTime(targetDateTime);
        config.setAndroidAppUrl(androidUrl.isEmpty() ? null : androidUrl);
        config.setIosAppUrl(iosUrl.isEmpty() ? null : iosUrl);
        config.setLaunchMessage(request.getLaunchMessage() != null ? request.getLaunchMessage().trim() : null);

        AppConfig updated = configRepo.save(config);
        log.info("Updated App Launch Config mode to: {}", mode);

        return ApiResponse.ok("Launch configuration updated successfully!", toLaunchDTO(updated));
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void checkAndAutoSwitchLaunchMode() {
        try {
            AppConfig config = configRepo.findById(CONFIG_ID).orElse(null);
            if (config != null
                    && "COMING_SOON".equalsIgnoreCase(config.getLaunchMode())
                    && config.getLaunchTargetDateTime() != null
                    && !LocalDateTime.now().isBefore(config.getLaunchTargetDateTime())) {
                log.info("Scheduled Check: Target time reached. Transitioning app status to LIVE_LAUNCHED.");
                config.setLaunchMode("LIVE_LAUNCHED");
                configRepo.save(config);
            }
        } catch (Exception e) {
            log.error("Error checking launch mode auto-switch timer", e);
        }
    }

    public BigDecimal getFarePerKm()              { return get().getFarePerKm(); }
    public BigDecimal getMinFare()                { return get().getMinFare(); }
    public int        getMatchRadiusMetres()      { return get().getMatchRadiusMetres(); }
    public int        getMatchTimeWindowMinutes() { return get().getMatchTimeWindowMinutes(); }

    private LaunchConfigDTO toLaunchDTO(AppConfig config) {
        return LaunchConfigDTO.builder()
                .launchMode(config.getLaunchMode() != null ? config.getLaunchMode() : "COMING_SOON")
                .launchTargetDateTime(config.getLaunchTargetDateTime())
                .androidAppUrl(config.getAndroidAppUrl())
                .iosAppUrl(config.getIosAppUrl())
                .launchMessage(config.getLaunchMessage())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}