package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.LiveSearchRequest;
import com.bikepooling.enums.RideState;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.RideStatusRepository;
import com.bikepooling.service.LiveRideService;
import com.bikepooling.service.LiveSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LiveRideController {

    private final LiveRideService    liveRideService;
    private final LiveSearchService  liveSearchService;
    private final RideStatusRepository rideStatusRepo;

    // ── Driver ────────────────────────────────────────────────────────────────

    /**
     * Driver taps "Go Live" on an OPEN ride.
     * Cancels pending applications, transitions OPEN → LIVE.
     */
    @PostMapping("/api/rides/{rideId}/live")
    public ResponseEntity<Void> goLive(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserPrincipal principal) {

        liveRideService.goLive(rideId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * Driver taps "Go Offline".
     * Transitions LIVE → OPEN. Blocked if already BOOKED.
     */
    @DeleteMapping("/api/rides/{rideId}/live")
    public ResponseEntity<Void> goOffline(
            @PathVariable Long rideId,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Guard: cannot go offline once a booker is confirmed
        rideStatusRepo.findByRideId(rideId).ifPresent(status -> {
            if (status.getState() == RideState.BOOKED) {
                throw AppException.conflict(
                        "Cannot go offline after confirming a booker. " +
                                "Complete or cancel the ride instead.");
            }
        });

        liveRideService.goOffline(rideId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    // ── Booker ────────────────────────────────────────────────────────────────

    @PostMapping("/api/live-search")
    public ResponseEntity<Void> startSearch(
            @Valid @RequestBody LiveSearchRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        liveSearchService.startSearch(req, principal.getUserId());
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/api/live-search")
    public ResponseEntity<Void> cancelSearch(
            @AuthenticationPrincipal UserPrincipal principal) {

        liveSearchService.cancelSearch(principal.getUserId());
        return ResponseEntity.ok().build();
    }
}