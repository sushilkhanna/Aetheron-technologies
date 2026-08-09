package com.bikepooling.controller;

import com.bikepooling.dto.request.AdminRideDTO;
import com.bikepooling.dto.request.AdminRideLocationDTO;
import com.bikepooling.dto.request.AdminRideStatsDTO;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.service.AdminRideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(ApiResponse.ok("Rides fetched",
                adminRideService.getRides(page, size, state, driverId, from, to, search)));
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
}