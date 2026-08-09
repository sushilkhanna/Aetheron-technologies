package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.ApplyRideRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.RideApplicantSummaryResponse;
import com.bikepooling.dto.response.RideApplicationResponse;
import com.bikepooling.service.RideApplicationService;
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
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideApplicationController {

    private final RideApplicationService applicationService;


    @PostMapping("/{rideId}/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RideApplicationResponse>> apply(
            @PathVariable Long rideId,
            @Valid @RequestBody ApplyRideRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RideApplicationResponse response = applicationService.apply(rideId, req, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Application submitted. Waiting for driver confirmation.", response));
    }


    @DeleteMapping("/applications/{applicationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        applicationService.withdraw(applicationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Application withdrawn.", null));
    }


    @GetMapping("/{rideId}/applications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RideApplicantSummaryResponse>>> listApplicants(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RideApplicantSummaryResponse> applicants =
                applicationService.listApplicants(rideId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Applicants fetched.", applicants));
    }


    @PostMapping("/applications/{applicationId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> confirm(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        applicationService.confirm(applicationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Booker confirmed. Ride is now booked.", null));
    }


    @PostMapping("/applications/{applicationId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        applicationService.reject(applicationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Applicant rejected.", null));
    }
}