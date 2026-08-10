package com.bikepooling.controller;

import com.bikepooling.dto.request.AdminRideLocationDTO;
import com.bikepooling.service.AdminRideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/admin/rides")
@RequiredArgsConstructor
public class AdminRideLocationStreamController {

    private final AdminRideService adminRideService;

    @GetMapping(value = "/{rideId}/location/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public SseEmitter streamRideLocation(@PathVariable Long rideId) {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean stopped = new AtomicBoolean(false);

        ScheduledExecutorService raw = Executors.newSingleThreadScheduledExecutor();
        ScheduledExecutorService scheduler = new DelegatingSecurityContextScheduledExecutorService(
                raw, SecurityContextHolder.getContext());

        Runnable push = () -> {
            if (stopped.get()) return;
            try {
                AdminRideLocationDTO loc = adminRideService.getRideLocation(rideId);
                if (loc != null) {
                    emitter.send(SseEmitter.event().data(loc, MediaType.APPLICATION_JSON));
                }
            } catch (IOException e) {
                if (!stopped.getAndSet(true)) {
                    log.debug("SSE client disconnected for rideId={}", rideId);
                    emitter.complete();
                    raw.shutdown();
                }
            } catch (Exception e) {
                if (!stopped.getAndSet(true)) {
                    log.error("Error pushing ride location for rideId={}", rideId, e);
                    emitter.completeWithError(e);
                    raw.shutdown();
                }
            }
        };

        scheduler.scheduleAtFixedRate(push, 0, 3, TimeUnit.SECONDS);

        emitter.onCompletion(() -> { if (!stopped.getAndSet(true)) raw.shutdown(); });
        emitter.onTimeout(() -> {
            if (!stopped.getAndSet(true)) { raw.shutdown(); emitter.complete(); }
        });
        emitter.onError(e -> { if (!stopped.getAndSet(true)) raw.shutdown(); });

        return emitter;
    }
}