package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.PostScheduledRideRequest;
import com.bikepooling.dto.request.SearchScheduledRideRequest;
import com.bikepooling.dto.request.UpdateScheduledRideRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ScheduledRideInstanceResponse;
import com.bikepooling.dto.response.ScheduledRideSearchResponse;
import com.bikepooling.dto.response.ScheduledRideTemplateResponse;
import com.bikepooling.service.ScheduledRideSearchService;
import com.bikepooling.service.ScheduledRideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduled-rides")
@RequiredArgsConstructor
public class ScheduledRideController {

    private final ScheduledRideService       scheduledRideService;
    private final ScheduledRideSearchService searchService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<ScheduledRideTemplateResponse>> postScheduledRide(
            @Valid @RequestBody PostScheduledRideRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ScheduledRideTemplateResponse resp =
                scheduledRideService.postScheduledRide(req, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Scheduled ride posted.", resp));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<ScheduledRideTemplateResponse>>> getMyScheduledRides(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Scheduled rides fetched.",
                scheduledRideService.getMyScheduledRides(principal.getUserId())));
    }

    @GetMapping("/{templateId}/instances")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ScheduledRideInstanceResponse>>> getInstancesForTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Instances fetched.",
                scheduledRideService.getInstancesForTemplate(templateId, principal.getUserId())));
    }

    @PatchMapping("/{templateId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<ScheduledRideTemplateResponse>> updateScheduledRide(
            @PathVariable Long templateId,
            @RequestBody UpdateScheduledRideRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ScheduledRideTemplateResponse resp =
                scheduledRideService.updateScheduledRide(templateId, req, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Scheduled ride updated.", resp));
    }

    @PostMapping("/{templateId}/cancel")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> cancelScheduledRide(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserPrincipal principal) {
        scheduledRideService.cancelScheduledRide(templateId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Scheduled ride cancelled.", null));
    }

    @PostMapping("/{templateId}/cancel-day")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> cancelScheduledRideDay(
            @PathVariable Long templateId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        scheduledRideService.cancelScheduledRideDay(templateId, date, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Day cancelled.", null));
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ScheduledRideSearchResponse>> search(
            @Valid @RequestBody SearchScheduledRideRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Scheduled rides found.",
                searchService.search(req, principal.getUserId())));
    }
}