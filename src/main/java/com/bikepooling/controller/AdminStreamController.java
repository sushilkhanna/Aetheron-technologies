package com.bikepooling.controller;

import com.bikepooling.dto.request.AdminMetricsDTO;
import com.bikepooling.service.AdminMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStreamController {

    private final AdminMetricsService metricsService;

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminMetricsDTO getMetrics() {
        return metricsService.getAll();
    }

    @GetMapping(value = "/metrics/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public SseEmitter streamMetrics() {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean stopped = new AtomicBoolean(false);

        // Wrap the scheduler with the current security context so the scheduler
        // thread carries valid authentication — prevents AuthorizationDeniedException
        // on Tomcat's async re-dispatch when SSE clients disconnect.
        ScheduledExecutorService raw = Executors.newSingleThreadScheduledExecutor();
        ScheduledExecutorService scheduler = new DelegatingSecurityContextScheduledExecutorService(
                raw, SecurityContextHolder.getContext());

        Runnable push = () -> {
            if (stopped.get()) return;
            try {
                AdminMetricsDTO data = metricsService.getAll();
                emitter.send(
                        SseEmitter.event()
                                .name("metrics")
                                .data(data, MediaType.APPLICATION_JSON)
                );
            } catch (IOException e) {
                // Client disconnected — stop cleanly, no error log needed
                if (!stopped.getAndSet(true)) {
                    log.debug("SSE client disconnected: rideId=stream");
                    emitter.complete();
                    raw.shutdown();
                }
            } catch (Exception e) {
                if (!stopped.getAndSet(true)) {
                    log.error("Error pushing SSE metrics", e);
                    emitter.completeWithError(e);
                    raw.shutdown();
                }
            }
        };

        scheduler.scheduleAtFixedRate(push, 0, 5, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            if (!stopped.getAndSet(true)) {
                log.debug("SSE emitter completed");
                raw.shutdown();
            }
        });

        emitter.onTimeout(() -> {
            if (!stopped.getAndSet(true)) {
                log.debug("SSE emitter timed out");
                raw.shutdown();
                emitter.complete();
            }
        });

        emitter.onError(e -> {
            if (!stopped.getAndSet(true)) {
                // Client closed tab/refreshed — normal, not an error
                log.debug("SSE emitter closed by client: {}", e.getMessage());
                raw.shutdown();
            }
        });

        return emitter;
    }
}