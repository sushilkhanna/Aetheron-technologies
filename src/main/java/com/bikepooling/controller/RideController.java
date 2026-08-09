package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.PostRideRequest;
import com.bikepooling.dto.request.RideSearchRequest;
import com.bikepooling.dto.request.UpdateRideRequest;
import com.bikepooling.dto.response.*;
import com.bikepooling.service.RideAlertService;
import com.bikepooling.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService      rideService;
    private final RideAlertService alertService;


    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RideResponse>>> postRide(
            @Valid @RequestBody PostRideRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RideResponse> rides = rideService.postRide(req, principal.getUserId());
        String msg = rides.size() == 2
                ? "Ride and return ride posted successfully."
                : "Ride posted successfully.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(msg, rides));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DriverRideDetailResponse>>> getMyActiveRides(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Active rides fetched.",
                rideService.getMyActiveRides(principal.getUserId())
        ));
    }


    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getMyRides(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Your rides fetched.",
                rideService.getDriverRides(principal.getUserId(), page, size)));
    }


    @GetMapping("/applications/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<BookerApplicationResponse>>> getMyApplications(
            @RequestParam(defaultValue = "active") String filter,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Applications fetched.",
                rideService.getBookerApplications(
                        principal.getUserId(), filter, from, to, page, size)));
    }


    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<RideSearchResponse>>> searchRides(
            @Valid @RequestBody RideSearchRequest req,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<RideSearchResponse> results = rideService.searchRides(
                req, principal.getUserId(), page, size);

        if (results.isEmpty() && req.isSaveAlertIfEmpty()) {
            alertService.saveAlert(req, principal.getUserId());
            return ResponseEntity.ok(ApiResponse.ok(
                    "No rides found. We'll notify you when one becomes available.",
                    results));
        }

        return ResponseEntity.ok(ApiResponse.ok("Rides found.", results));
    }

    @PatchMapping("/{rideId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RideResponse>> updateRide(
            @PathVariable Long rideId,
            @Valid @RequestBody UpdateRideRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Ride updated.",
                rideService.updateRide(rideId, req, principal.getUserId())));
    }

    @PostMapping("/{rideId}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> startRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        rideService.startRide(rideId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Ride started.", null));
    }

    @GetMapping("/{rideId}/otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getBookerOtp(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String otp = rideService.getBookerOtp(rideId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("OTP fetched.", otp));
    }

    @PostMapping("/{rideId}/verify-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyRideOtp(
            @PathVariable Long rideId,
            @RequestParam String otp,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        rideService.verifyRideOtp(rideId, otp, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("OTP verified. Ride is in progress.", null));
    }

    @PostMapping("/{rideId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> completeRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        rideService.completeRide(rideId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Ride completed.", null));
    }

    @PostMapping("/{rideId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancelRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        rideService.cancelRide(rideId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Ride cancelled.", null));
    }
}