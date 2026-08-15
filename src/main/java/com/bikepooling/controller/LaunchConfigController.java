package com.bikepooling.controller;

import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.LaunchConfigDTO;
import com.bikepooling.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/launch-config")
@RequiredArgsConstructor
public class LaunchConfigController {

    private final AppConfigService appConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<LaunchConfigDTO>> getLaunchConfig() {
        LaunchConfigDTO config = appConfigService.getLaunchConfig();
        return ResponseEntity.ok(ApiResponse.ok("Launch config fetched successfully", config));
    }
}
