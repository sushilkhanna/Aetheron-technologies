package com.bikepooling.service;

import com.bikepooling.dto.request.RideSearchRequest;
import com.bikepooling.entity.Ride;
import com.bikepooling.entity.RideAlert;
import com.bikepooling.entity.User;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.RideAlertRepository;
import com.bikepooling.repository.UserRepository;
import com.bikepooling.util.RouteMatchUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideAlertService {

    private final RideAlertRepository alertRepo;
    private final UserRepository      userRepo;
    private final FcmService          fcmService;

    // ── save alert ────────────────────────────────────────────────────────────

    @Transactional
    public RideAlert saveAlert(RideSearchRequest req, Long userId) {

        if (!req.getWindowTo().isAfter(req.getWindowFrom())) {
            throw AppException.badRequest("Window end time must be after window start time.");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        RideAlert alert = RideAlert.builder()
                .user(user)
                .sourceName(req.getSourceName())
                .sourceLat(req.getSourceLat())
                .sourceLng(req.getSourceLng())
                .destinationName(req.getDestinationName())
                .destinationLat(req.getDestinationLat())
                .destinationLng(req.getDestinationLng())
                .windowFrom(req.getWindowFrom())
                .windowTo(req.getWindowTo())
                .expiresAt(req.getWindowTo()) // alert expires when time window ends
                .active(true)
                .build();

        alert = alertRepo.save(alert);
        log.info("Ride alert saved: id={} for userId={}", alert.getId(), userId);
        return alert;
    }

    // ── deactivate alert ──────────────────────────────────────────────────────

    @Transactional
    public void deactivateAlert(Long alertId, Long userId) {
        RideAlert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> AppException.notFound("Alert not found"));

        if (!alert.getUser().getId().equals(userId)) {
            throw AppException.forbidden("You can only delete your own alerts.");
        }

        alert.setActive(false);
        alertRepo.save(alert);
    }


    @Async
    public void matchAlertsForNewRide(Ride ride) {
        List<RideAlert> candidates = alertRepo.findActiveAlertsForTime(
                LocalDateTime.now(),
                ride.getDepartAt()
        );

        for (RideAlert alert : candidates) {
            RouteMatchUtil.MatchResult stage1 = RouteMatchUtil.evaluateStage1(
                    ride.getFromLat().doubleValue(), ride.getFromLng().doubleValue(),
                    ride.getToLat().doubleValue(),   ride.getToLng().doubleValue(),
                    ride.getDistanceKm().doubleValue(),
                    alert.getSourceLat().doubleValue(), alert.getSourceLng().doubleValue(),
                    alert.getDestinationLat().doubleValue(), alert.getDestinationLng().doubleValue()
            );
            if (!stage1.isMatched()) continue;

            log.info("Alert match: alertId={} rideId={} userId={}",
                    alert.getId(), ride.getId(), alert.getUser().getId());

            try {
                fcmService.notifyAlertMatch(alert.getUser().getId(), ride.getFromName(), ride.getToName(), ride.getId());
            } catch (Exception e) {
                log.warn("Alert match notification failed: alertId={} err={}", alert.getId(), e.getMessage());
            }

            alert.setActive(false);
            alertRepo.save(alert);
        }
    }
}