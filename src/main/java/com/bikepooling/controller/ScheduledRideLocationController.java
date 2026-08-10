package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ScheduledRideLocationBroadcast;
import com.bikepooling.enums.RideState;
import com.bikepooling.repository.ScheduledRideInstanceRepository;
import com.bikepooling.service.ScheduledRideTrackingService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ScheduledRideLocationController {

    private final SimpMessagingTemplate              messagingTemplate;
    private final ScheduledRideInstanceRepository     instanceRepo;
    private final ScheduledRideTrackingService       trackingService;

    private static final String LOCATION_TOPIC = "/topic/scheduled-rides/instances/%d/location";

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledRideLocationMessage {
        private double lat;
        private double lng;
        private Double bearingDegrees;
        private Double speedKmh;
        private long timestamp;
        private Long driverId;
    }

    /**
     * WebSocket STOMP Endpoint: /app/scheduled-rides/instances/{instanceId}/location
     */
    @MessageMapping("/scheduled-rides/instances/{instanceId}/location")
    public void relayLocation(
            @DestinationVariable Long instanceId,
            ScheduledRideLocationMessage msg,
            Principal principal) {

        Long driverId = null;
        if (principal != null) {
            try { driverId = Long.parseLong(principal.getName()); } catch (Exception ignored) {}
        }
        if (driverId == null) {
            driverId = msg.getDriverId();
        }

        if (msg.getLat() < -90 || msg.getLat() > 90 || msg.getLng() < -180 || msg.getLng() > 180) {
            log.warn("Invalid coordinates for instanceId={}", instanceId);
            return;
        }

        processAndBroadcastScheduledRideLocation(instanceId, msg, driverId);
    }

    /**
     * REST Fallback Endpoint: POST /api/scheduled-rides/instances/{instanceId}/location
     */
    @PostMapping("/api/scheduled-rides/instances/{instanceId}/location")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> pushScheduledLocationRest(
            @PathVariable Long instanceId,
            @RequestBody ScheduledRideLocationMessage msg,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long driverId = principal != null ? principal.getUserId() : msg.getDriverId();
        processAndBroadcastScheduledRideLocation(instanceId, msg, driverId);

        return ResponseEntity.ok(ApiResponse.ok("Location updated in Redis cache.", null));
    }

    private void processAndBroadcastScheduledRideLocation(Long instanceId, ScheduledRideLocationMessage msg, Long driverId) {
        instanceRepo.findByIdWithDetails(instanceId).ifPresent(instance -> {
            RideState state = instance.getState();

            // Store location in Redis cache & evaluate auto-completion / off-route safety alerts
            trackingService.processDriverLocation(
                    instanceId,
                    msg.getLat(),
                    msg.getLng(),
                    msg.getBearingDegrees(),
                    msg.getSpeedKmh(),
                    msg.getTimestamp() > 0 ? msg.getTimestamp() : System.currentTimeMillis(),
                    driverId
            );

            // Stream location to STOMP topic (STARTED & SOS_TRIGGERED)
            if (state == RideState.STARTED || state == RideState.SOS_TRIGGERED) {
                ScheduledRideLocationBroadcast broadcast = ScheduledRideLocationBroadcast.builder()
                        .instanceId(instanceId)
                        .lat(msg.getLat())
                        .lng(msg.getLng())
                        .bearingDegrees(msg.getBearingDegrees())
                        .speedKmh(msg.getSpeedKmh())
                        .timestamp(msg.getTimestamp() > 0 ? msg.getTimestamp() : System.currentTimeMillis())
                        .driverName(firstNameOnly(instance.getTemplate().getPostedBy().getFullName()))
                        .state(state)
                        .build();

                messagingTemplate.convertAndSend(String.format(LOCATION_TOPIC, instanceId), broadcast);
            }
        });
    }

    private static String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Driver";
        return fullName.trim().split("\\s+")[0];
    }
}
