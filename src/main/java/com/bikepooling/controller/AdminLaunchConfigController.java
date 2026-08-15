package com.bikepooling.controller;

import com.bikepooling.dto.request.UpdateLaunchConfigRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.LaunchConfigDTO;
import com.bikepooling.service.AppConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/launch-config")
@RequiredArgsConstructor
public class AdminLaunchConfigController {

    private final AppConfigService appConfigService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LaunchConfigDTO>> getLaunchConfig() {
        LaunchConfigDTO config = appConfigService.getLaunchConfig();
        return ResponseEntity.ok(ApiResponse.ok("Admin launch config fetched successfully", config));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LaunchConfigDTO>> updateLaunchConfig(
            @RequestBody @Valid UpdateLaunchConfigRequest request) {

        ApiResponse<LaunchConfigDTO> response = appConfigService.updateLaunchConfig(request);
        return ResponseEntity.ok(response);
    }
}
