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

    // ── pre-built notification helpers ────────────────────────────────────────

    public void notifyDriverNewApplicant(Long driverUserId, String bookerName, Long rideId) {
        sendToUser(
                driverUserId,
                "New ride applicant",
                bookerName + " wants to join your ride.",
                Map.of("type", "NEW_APPLICANT", "rideId", String.valueOf(rideId))
        );
    }

    public void notifyBookerApplicationConfirmed(Long bookerUserId, String driverName, Long rideId) {
        sendToUser(
                bookerUserId,
                "Ride confirmed!",
                driverName + " accepted your ride request.",
                Map.of("type", "APPLICATION_CONFIRMED", "rideId", String.valueOf(rideId))
        );
    }

    public void notifyBookerApplicationRejected(Long bookerUserId, String driverName, Long rideId) {
        sendToUser(
                bookerUserId,
                "Ride request declined",
                driverName + " could not accommodate your request.",
                Map.of("type", "APPLICATION_REJECTED", "rideId", String.valueOf(rideId))
        );
    }

    public void notifyDriverBookerCancelled(Long driverUserId, String bookerName, Long rideId) {
        sendToUser(
                driverUserId,
                "Booking cancelled",
                bookerName + " has cancelled their booking.",
                Map.of("type", "BOOKING_CANCELLED_BY_BOOKER", "rideId", String.valueOf(rideId))
        );
    }

    public void notifyBookerRideCancelled(Long bookerUserId, String driverName, Long rideId) {
        sendToUser(
                bookerUserId,
                "Ride cancelled",
                driverName + " has cancelled the ride.",
                Map.of("type", "RIDE_CANCELLED_BY_DRIVER", "rideId", String.valueOf(rideId))
        );
    }

    public void notifyAlertMatch(Long userId, String fromName, String toName, Long rideId) {
        sendToUser(
                userId,
                "Ride available!",
                "A ride from " + fromName + " to " + toName + " just became available.",
                Map.of("type", "ALERT_MATCH", "rideId", String.valueOf(rideId))
        );
    }

    public void notifyDriverRideRequest(Long driverUserId, String bookerName,
                                        String pickupName, String dropName,
                                        Long requestId, Long rideId) {
        sendToUser(
                driverUserId,
                "New ride request near your route",
                bookerName + " wants to go from " + pickupName + " to " + dropName + ".",
                Map.of(
                        "type", "RIDE_REQUEST_MATCH",
                        "requestId", String.valueOf(requestId),
                        "rideId", String.valueOf(rideId)
                )
        );
    }

    public void notifyBookerRideStarted(
            Long bookerUserId,
            String driverName,
            Long rideId
    ) {
        sendToUser(
                bookerUserId,
                "Driver started the ride",
                driverName + " has started the ride and is heading towards you.",
                Map.of(
                        "type", "RIDE_STARTED",
                        "rideId", String.valueOf(rideId)
                )
        );
    }

    public void notifyBookerRideCompleted(
            Long bookerUserId,
            String driverName,
            Long rideId
    ) {
        sendToUser(
                bookerUserId,
                "Ride completed",
                "Your ride with " + driverName +
                        " has been completed safely. Please rate your experience.",
                Map.of(
                        "type", "RIDE_COMPLETED",
                        "rideId", String.valueOf(rideId)
                )
        );
    }

    public void notifyDriverLiveBookerFound(Long driverId,
                                            Long bookerId,
                                            Long rideId,
                                            String pickupName,
                                            String dropName,
                                            int etaMinutes) {

        List<String> tokens = fcmTokenRepo.findByUserId(driverId)   // ← correct method name
                .stream()
                .map(com.bikepooling.entity.FcmToken::getToken)
                .toList();

        if (tokens.isEmpty()) {
            log.warn("No FCM tokens for driverId={}", driverId);
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .putData("type",       "LIVE_BOOKER_FOUND")
                .putData("rideId",     String.valueOf(rideId))
                .putData("bookerId",   String.valueOf(bookerId))
                .putData("pickupName", pickupName != null ? pickupName : "")
                .putData("dropName",   dropName   != null ? dropName   : "")
                .putData("etaMinutes", String.valueOf(etaMinutes))
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("Live booker FCM sent to driverId={} rideId={}: {}/{} success",
                    driverId, rideId, response.getSuccessCount(), tokens.size());
        } catch (FirebaseMessagingException e) {
            log.error("FCM failed for driverId={}: {}", driverId, e.getMessage());
        }
    }

    public void notifyBookerApplicationRejectedDriverWentLive(Long bookerId,
                                                              String driverName,
                                                              Long rideId) {
        List<String> tokens = fcmTokenRepo.findByUserId(bookerId)
                .stream().map(FcmToken::getToken).toList();
        if (tokens.isEmpty()) return;

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .putData("type",       "APPLICATION_REJECTED_DRIVER_LIVE")
                .putData("rideId",     String.valueOf(rideId))
                .putData("driverName", driverName != null ? driverName : "")
                .putData("reason",     "Driver started live mode on this ride")
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("Notified bookerId={} application rejected (driver went live) rideId={}",
                    bookerId, rideId);
        } catch (FirebaseMessagingException e) {
            log.error("FCM failed for bookerId={}: {}", bookerId, e.getMessage());
        }
    }

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