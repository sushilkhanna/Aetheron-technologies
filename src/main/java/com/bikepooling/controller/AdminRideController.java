package com.bikepooling.controller;

import com.bikepooling.dto.request.AdminRideDTO;
import com.bikepooling.dto.request.AdminRideLocationDTO;
import com.bikepooling.dto.request.AdminRideStatsDTO;
import com.bikepooling.dto.request.SendRideNotificationRequest;
import com.bikepooling.dto.response.AdminMessageResponse;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.service.AdminRideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/rides")
@RequiredArgsConstructor
public class AdminRideController {

    private final AdminRideService adminRideService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AdminRideDTO>>> getRides(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String state,
            @RequestParam(required = false)    Long driverId,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String sortBy,
            @RequestParam(required = false)    String sortDir,
            @RequestParam(required = false)    String from,
            @RequestParam(required = false)    String to) {

        return ResponseEntity.ok(ApiResponse.ok("Rides fetched",
                adminRideService.getRides(page, size, state, driverId, from, to, search, sortBy, sortDir)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminRideStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Stats fetched",
                adminRideService.getStats()));
    }

    @GetMapping("/{rideId}/location")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminRideLocationDTO>> getRideLocation(
            @PathVariable Long rideId) {

        AdminRideLocationDTO loc = adminRideService.getRideLocation(rideId);
        if (loc == null) {
            return ResponseEntity.ok(ApiResponse.ok("No active location data", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("Location fetched", loc));
    }

    @GetMapping("/active-locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminRideLocationDTO>>> getAllActiveLocations() {
        return ResponseEntity.ok(ApiResponse.ok("Active ride locations fetched",
                adminRideService.getAllActiveLocations()));
    }

    @PostMapping("/send-notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminMessageResponse>> sendRideNotification(
            @RequestBody @Valid SendRideNotificationRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Ride notifications processed",
                adminRideService.sendRideNotification(request)));
    }
}