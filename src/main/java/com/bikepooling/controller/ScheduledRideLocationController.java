package com.bikepooling.controller;

import com.bikepooling.dto.response.ScheduledRideLocationBroadcast;
import com.bikepooling.enums.RideState;
import com.bikepooling.repository.ScheduledRideInstanceRepository;
import com.bikepooling.service.ScheduledRideTrackingService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ScheduledRideLocationController {

    private final SimpMessagingTemplate              messagingTemplate;
    private final ScheduledRideInstanceRepository     instanceRepo;
    private final ScheduledRideTrackingService       trackingService;

    private static final String LOCATION_TOPIC = "/topic/scheduled-rides/instances/%d/location";

    @Getter
    @Builder
    public static class ScheduledRideLocationMessage {
        private double lat;
        private double lng;
        private Double bearingDegrees;
        private Double speedKmh;
        private long timestamp;
    }

    @MessageMapping("/scheduled-rides/instances/{instanceId}/location")
    public void relayLocation(
            @DestinationVariable Long instanceId,
            ScheduledRideLocationMessage msg,
            Principal principal) {

        if (principal == null) return;
        Long driverId = Long.parseLong(principal.getName());

        if (msg.getLat() < -90 || msg.getLat() > 90 || msg.getLng() < -180 || msg.getLng() > 180) {
            log.warn("Invalid coordinates from driverId={} instanceId={}", driverId, instanceId);
            return;
        }

        instanceRepo.findByIdWithDetails(instanceId).ifPresent(instance -> {
            // Guard: only driver of template can send location
            if (!instance.getTemplate().getPostedBy().getId().equals(driverId)) {
                log.warn("Unauthorized location push for instanceId={} by userId={}", instanceId, driverId);
                return;
            }

            RideState state = instance.getState();

            // Store location in cache & evaluate auto-completion / off-route safety alerts
            trackingService.processDriverLocation(
                    instanceId,
                    msg.getLat(),
                    msg.getLng(),
                    msg.getBearingDegrees(),
                    msg.getSpeedKmh(),
                    msg.getTimestamp() > 0 ? msg.getTimestamp() : System.currentTimeMillis(),
                    driverId
            );

            // Stream location to STOMP topic:
            // 1. STARTED: Send to booker so booker sees driver coming to pick them up
            // 2. VERIFIED: Kept in cache for safety monitoring (stop broadcasting to booker)
            // 3. SOS_TRIGGERED: Broadcast live for emergency monitoring
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
