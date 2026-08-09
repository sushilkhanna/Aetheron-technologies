package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.SosLocationPingRequest;
import com.bikepooling.dto.request.SosTriggerRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.SosActiveSummaryResponse;
import com.bikepooling.dto.response.SosAlertResponse;
import com.bikepooling.dto.response.SosLocationPingResponse;
import com.bikepooling.service.SosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;

    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<SosAlertResponse>> trigger(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SosTriggerRequest req) {

        SosAlertResponse response = sosService.trigger(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("SOS triggered.", response));
    }

    @PostMapping("/{alertId}/ping")
    public ResponseEntity<ApiResponse<Void>> ping(
            @PathVariable Long alertId,
            @Valid @RequestBody SosLocationPingRequest req) {

        sosService.addLocationPing(alertId, req);
        return ResponseEntity.ok(ApiResponse.ok("Location recorded.", null));
    }

    @GetMapping("/{alertId}/trail")
    public ResponseEntity<ApiResponse<List<SosLocationPingResponse>>> getTrail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long alertId) {

        return ResponseEntity.ok(ApiResponse.ok("Trail fetched.",
                sosService.getLocationTrail(principal.getUserId(), alertId)));
    }

    @GetMapping("/track/{token}")
    public ResponseEntity<ApiResponse<List<SosLocationPingResponse>>> getTrailByToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok("Trail fetched.", sosService.getLocationTrailByToken(token)));
    }

    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<ApiResponse<SosAlertResponse>> resolve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long alertId) {

        return ResponseEntity.ok(ApiResponse.ok("SOS resolved.",
                sosService.resolve(principal.getUserId(), alertId, false)));
    }

    @PostMapping("/{alertId}/false-alarm")
    public ResponseEntity<ApiResponse<SosAlertResponse>> falseAlarm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long alertId) {

        return ResponseEntity.ok(ApiResponse.ok("SOS marked as false alarm.",
                sosService.resolve(principal.getUserId(), alertId, true)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SosActiveSummaryResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok("Active SOS alerts fetched.", sosService.getActiveAlerts()));
    }
}