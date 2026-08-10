package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.service.LiveRideTrackingService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LiveRideLocationController {

    private final LiveRideTrackingService trackingService;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveRideLocationMessage {
        private double lat;
        private double lng;
        private Double bearingDegrees;
        private Double speedKmh;
        private long timestamp;
        private Long driverId;
    }

    /**
     * WebSocket STOMP Endpoint: /app/live-rides/{liveRideId}/location
     */
    @MessageMapping("/live-rides/{liveRideId}/location")
    public void relayLiveLocation(
            @DestinationVariable Long liveRideId,
            LiveRideLocationMessage msg,
            Principal principal) {

        Long driverId = null;
        if (principal != null) {
            try { driverId = Long.parseLong(principal.getName()); } catch (Exception ignored) {}
        }
        if (driverId == null) {
            driverId = msg.getDriverId();
        }

        if (msg.getLat() < -90 || msg.getLat() > 90 || msg.getLng() < -180 || msg.getLng() > 180) {
            log.warn("Invalid live location coordinates for liveRideId={}", liveRideId);
            return;
        }

        trackingService.processLiveDriverLocation(
                liveRideId,
                msg.getLat(),
                msg.getLng(),
                msg.getBearingDegrees(),
                msg.getSpeedKmh(),
                msg.getTimestamp() > 0 ? msg.getTimestamp() : System.currentTimeMillis(),
                driverId
        );
    }

    /**
     * REST Fallback Endpoint: POST /api/live-rides/{liveRideId}/location
     */
    @PostMapping("/api/live-rides/{liveRideId}/location")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> pushLiveLocationRest(
            @PathVariable Long liveRideId,
            @RequestBody LiveRideLocationMessage msg,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long driverId = principal != null ? principal.getUserId() : msg.getDriverId();

        trackingService.processLiveDriverLocation(
                liveRideId,
                msg.getLat(),
                msg.getLng(),
                msg.getBearingDegrees(),
                msg.getSpeedKmh(),
                msg.getTimestamp() > 0 ? msg.getTimestamp() : System.currentTimeMillis(),
                driverId
        );

        return ResponseEntity.ok(ApiResponse.ok("Location updated in Redis cache.", null));
    }
}
