package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.*;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.LiveRidePreviewResponse;
import com.bikepooling.dto.response.LiveRideResponse;
import com.bikepooling.service.LiveRideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/live-rides")
@RequiredArgsConstructor
public class LiveRideController {

    private final LiveRideService liveRideService;

    @PostMapping("/go-live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LiveRideResponse>> goLive(
            @Valid @RequestBody GoLiveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Live mode started.",
                liveRideService.goLive(principal.getUserId(), req)));
    }

    @PostMapping("/stop-live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> stopLive(
            @AuthenticationPrincipal UserPrincipal principal) {
        liveRideService.stopLive(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Live mode stopped.", null));
    }

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LiveRidePreviewResponse>> previewFare(
            @Valid @RequestBody LiveRidePreviewRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fare preview calculated.",
                liveRideService.previewFare(req)));
    }

    @PostMapping("/search/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> startSearch(
            @Valid @RequestBody LiveRideSearchRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long searchRequestId = liveRideService.startSearch(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Live search started. Notified matching drivers nearby.", searchRequestId));
    }

    @PostMapping("/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LiveRideResponse>> acceptRide(
            @Valid @RequestBody LiveRideAcceptRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Ride request accepted.",
                liveRideService.acceptRide(principal.getUserId(), req)));
    }

    @PostMapping("/{liveRideId}/verify-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LiveRideResponse>> verifyOtp(
            @PathVariable Long liveRideId,
            @RequestParam String otp,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "OTP verified successfully.",
                liveRideService.verifyOtp(principal.getUserId(), liveRideId, otp)));
    }

    @PostMapping("/{liveRideId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LiveRideResponse>> completeRide(
            @PathVariable Long liveRideId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Live ride completed.",
                liveRideService.completeRide(principal.getUserId(), liveRideId)));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LiveRideResponse>> getMyActiveRide(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Active live ride fetched.",
                liveRideService.getMyActiveRide(principal.getUserId())));
    }
}
