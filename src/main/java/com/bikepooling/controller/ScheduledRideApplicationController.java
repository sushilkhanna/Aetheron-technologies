package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.ApplicationDaysRequest;
import com.bikepooling.dto.request.ApplyScheduledRideRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ScheduledRideApplicantResponse;
import com.bikepooling.dto.response.ScheduledRideApplicationDayResponse;
import com.bikepooling.dto.response.ScheduledRideBookerApplicationResponse;
import com.bikepooling.service.ScheduledRideApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/scheduled-rides")
@RequiredArgsConstructor
public class ScheduledRideApplicationController {

    private final ScheduledRideApplicationService applicationService;

    @PostMapping("/{templateId}/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ScheduledRideApplicationDayResponse>>> apply(
            @PathVariable Long templateId,
            @Valid @RequestBody ApplyScheduledRideRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Application submitted.",
                applicationService.apply(templateId, req, principal.getUserId())));
    }

    @GetMapping("/{templateId}/applicants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ScheduledRideApplicantResponse>>> listApplicants(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Applicants fetched.",
                applicationService.listApplicants(templateId, principal.getUserId())));
    }

    @GetMapping("/my-applications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ScheduledRideBookerApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "My applications fetched.",
                applicationService.getMyApplications(principal.getUserId())));
    }

    // ── Confirmation Endpoint (Driver) ────────────────────────────────────────

    @PostMapping("/applications/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> confirmDays(
            @Valid @RequestBody ApplicationDaysRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.confirmDays(req.getApplicationDayIds(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Selected days confirmed.", null));
    }

    // ── Rejection Endpoint (Driver) ───────────────────────────────────────────

    @PostMapping("/applications/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> rejectDays(
            @Valid @RequestBody ApplicationDaysRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.rejectDays(req.getApplicationDayIds(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Selected days rejected.", null));
    }

    // ── Withdrawal Endpoint (Booker) ──────────────────────────────────────────

    @PostMapping("/applications/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> withdrawDays(
            @Valid @RequestBody ApplicationDaysRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.withdrawDays(req.getApplicationDayIds(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Selected days withdrawn.", null));
    }

    // ── Instance Lifecycle Endpoints ──────────────────────────────────────────

    @PostMapping("/instances/{instanceId}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> start(
            @PathVariable Long instanceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.startInstance(instanceId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Ride started.", null));
    }

    @PostMapping("/instances/{instanceId}/verify-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @PathVariable Long instanceId,
            @RequestParam String otp,
            @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.verifyInstanceOtp(instanceId, otp, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("OTP verified.", null));
    }

    @PostMapping("/instances/{instanceId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> complete(
            @PathVariable Long instanceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.completeInstance(instanceId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Ride completed.", null));
    }
}
