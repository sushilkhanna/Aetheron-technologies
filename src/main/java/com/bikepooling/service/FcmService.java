package com.bikepooling.service;

import com.bikepooling.entity.FcmToken;
import com.bikepooling.entity.User;
import com.bikepooling.repository.FcmTokenRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepo;


    @Async
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        List<FcmToken> tokens = fcmTokenRepo.findByUserId(userId);

        if (tokens.isEmpty()) {
            log.debug("No FCM tokens for userId={}", userId);
            return;
        }

        for (FcmToken fcmToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data != null ? data : Map.of())
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.debug("FCM sent to userId={} token={} response={}", userId,
                        fcmToken.getToken().substring(0, 10) + "...", response);

            } catch (FirebaseMessagingException e) {
                // token is invalid or unregistered — remove it
                if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT
                        || e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    log.warn("Stale FCM token removed for userId={}", userId);
                    fcmTokenRepo.deleteByToken(fcmToken.getToken());
                } else {
                    log.error("FCM error for userId={}: {}", userId, e.getMessage());
                }
            }
        }
    }

    // ── Scheduled Ride Notification Helpers ───────────────────────────

    public void notifyDriverScheduledApplicant(
            Long driverId,
            String bookerName,
            Long templateId,
            LocalDate date) {

        sendToUser(
                driverId,
                "New scheduled ride request",
                bookerName + " applied for " + date + ".",
                Map.of(
                        "type","SCHEDULED_APPLICANT",
                        "templateId",String.valueOf(templateId),
                        "date",date.toString()
                )
        );
    }

    public void notifyBookerScheduledDayConfirmed(
            Long bookerId,
            String driverName,
            Long instanceId,
            LocalDate date) {

        sendToUser(
                bookerId,
                "Ride confirmed",
                driverName + " accepted your request for " + date + ".",
                Map.of(
                        "type","SCHEDULED_DAY_CONFIRMED",
                        "instanceId",String.valueOf(instanceId),
                        "date",date.toString()
                )
        );
    }

    public void notifyBookerScheduledDayUnavailable(
            Long bookerId,
            String driverName,
            LocalDate date,
            boolean otherDaysStillActive) {

        sendToUser(
                bookerId,
                "Ride unavailable",
                driverName + " selected another passenger for " + date + ".",
                Map.of(
                        "type","SCHEDULED_DAY_UNAVAILABLE",
                        "date",date.toString(),
                        "otherDaysStillActive",
                        String.valueOf(otherDaysStillActive)
                )
        );
    }

    public void notifyBookerScheduledDayRejected(
            Long bookerId,
            String driverName,
            LocalDate date) {

        sendToUser(
                bookerId,
                "Request declined",
                driverName + " declined your request for " + date + ".",
                Map.of(
                        "type","SCHEDULED_DAY_REJECTED",
                        "date",date.toString()
                )
        );
    }

    public void notifyBookerScheduledRideCancelled(
            Long bookerId,
            String driverName,
            LocalDate date) {

        sendToUser(
                bookerId,
                "Ride cancelled",
                driverName + " cancelled the ride scheduled on "
                        + date + ".",
                Map.of(
                        "type","SCHEDULED_RIDE_CANCELLED",
                        "date",date.toString()
                )
        );
    }

    public void notifyBookerScheduledRideStarted(
            Long bookerId,
            String driverName,
            Long instanceId) {

        sendToUser(
                bookerId,
                "Ride started",
                driverName + " has started the ride.",
                Map.of(
                        "type","SCHEDULED_RIDE_STARTED",
                        "instanceId",String.valueOf(instanceId)
                )
        );
    }

    public void notifyBookerScheduledRideCompleted(
            Long bookerId,
            String driverName,
            Long instanceId) {

        sendToUser(
                bookerId,
                "Ride completed",
                "Your ride with "
                        + driverName
                        + " has been completed safely.",
                Map.of(
                        "type","SCHEDULED_RIDE_COMPLETED",
                        "instanceId",
                        String.valueOf(instanceId)
                )
        );
    }


}