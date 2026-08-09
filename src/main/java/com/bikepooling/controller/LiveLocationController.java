package com.bikepooling.controller;

import com.bikepooling.dto.request.LiveRideMeta;
import com.bikepooling.dto.request.LocationUpdateMessage;
import com.bikepooling.dto.response.LocationBroadcastMessage;
import com.bikepooling.enums.RideState;
import com.bikepooling.repository.LiveRideRedisRepository;
import com.bikepooling.repository.RideStatusRepository;
import com.bikepooling.service.LiveRideService;
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
public class LiveLocationController {

    private final SimpMessagingTemplate   messagingTemplate;
    private final RideStatusRepository    rideStatusRepo;
    private final LiveRideRedisRepository liveRideRepo;
    private final LiveRideService         liveRideService;

    private static final String LOCATION_TOPIC = "/topic/ride/%d/location";

    @MessageMapping("/ride/{rideId}/location")
    public void relayLocation(@DestinationVariable Long rideId,
                              LocationUpdateMessage msg,
                              Principal principal) {

        Long driverId = Long.parseLong(principal.getName());

        if (msg.getLat() < -90 || msg.getLat() > 90
                || msg.getLng() < -180 || msg.getLng() > 180) {
            log.warn("Invalid coordinates from driverId={} rideId={}", driverId, rideId);
            return;
        }

        LiveRideMeta meta = liveRideRepo.findMeta(rideId);
        if (meta == null) {
            log.debug("No Redis meta for rideId={}, ignoring location push", rideId);
            return;
        }
        if (!meta.getDriverId().equals(driverId)) {
            log.warn("Unauthorized location push: userId={} rideId={}", driverId, rideId);
            return;
        }

        RideState state = resolveState(meta, rideId);
        if (state == null) return;

        switch (state) {

            case LIVE -> liveRideService.updateLocation(
                    rideId, driverId, msg.getLat(), msg.getLng(),
                    msg.getBearingDegrees(), msg.getSpeedKmh(), state);

            case STARTED -> {
                liveRideService.updateLocation(
                        rideId, driverId, msg.getLat(), msg.getLng(),
                        msg.getBearingDegrees(), msg.getSpeedKmh(), state);

                rideStatusRepo.findByRideIdWithDetails(rideId).ifPresent(status -> {
                    LocationBroadcastMessage broadcast = LocationBroadcastMessage.builder()
                            .lat(msg.getLat())
                            .lng(msg.getLng())
                            .bearingDegrees(msg.getBearingDegrees())
                            .speedKmh(msg.getSpeedKmh())
                            .timestamp(msg.getTimestamp())
                            .rideId(rideId)
                            .driverName(firstNameOnly(
                                    status.getRide().getPostedBy().getFullName()))
                            .build();
                    messagingTemplate.convertAndSend(
                            String.format(LOCATION_TOPIC, rideId), broadcast);
                });
            }

            case VERIFIED -> liveRideService.updateLocation(
                    rideId, driverId, msg.getLat(), msg.getLng(),
                    msg.getBearingDegrees(), msg.getSpeedKmh(), state);

            case BOOKED -> log.debug("Location ignored in BOOKED state: rideId={}", rideId);

            default -> log.debug("Location push ignored for state={} rideId={}", state, rideId);
        }
    }

    private RideState resolveState(LiveRideMeta meta, Long rideId) {
        if (meta.getCurrentState() != null) {
            try {
                return RideState.valueOf(meta.getCurrentState());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid currentState '{}' in Redis for rideId={}",
                        meta.getCurrentState(), rideId);
                return null;
            }
        }

        return rideStatusRepo.findByRideId(rideId).map(status -> {
            RideState state = status.getState();
            meta.setCurrentState(state.name());
            liveRideRepo.saveMeta(meta);
            log.debug("currentState seeded from DB for rideId={}: {}", rideId, state);
            return state;
        }).orElseGet(() -> {
            log.warn("RideStatus not found in DB for rideId={}", rideId);
            return null;
        });
    }

    private static String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Driver";
        return fullName.trim().split("\\s+")[0];
    }
}