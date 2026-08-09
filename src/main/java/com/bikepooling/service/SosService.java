package com.bikepooling.service;

import com.bikepooling.dto.request.SosLocationPingRequest;
import com.bikepooling.dto.request.SosTriggerRequest;
import com.bikepooling.dto.response.SosActiveSummaryResponse;
import com.bikepooling.dto.response.SosAlertResponse;
import com.bikepooling.dto.response.SosLocationPingResponse;
import com.bikepooling.entity.*;
import com.bikepooling.enums.RideState;
import com.bikepooling.enums.Role;
import com.bikepooling.enums.SosStatus;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SosService {

    private static final int EXPIRY_INACTIVITY_MINUTES = 30;

    private final SosRepository sosRepository;
    private final SosLocationPingRepository sosLocationPingRepository;
    private final RideRepository rideRepository;
    private final RideStatusRepository rideStatusRepository;
    private final UserRepository userRepository;
    private final Msg91SmsClient smsClient;
    private final FcmService fcmService;

    @Transactional
    public SosAlertResponse trigger(Long userId, SosTriggerRequest req) {
        Ride ride = rideRepository.findById(req.getRideId())
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        User triggeredBy = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        RideStatus rideStatus = rideStatusRepository.findByRide_Id(ride.getId())
                .orElseThrow(() -> AppException.notFound("Ride status not found"));

        String role = resolveRole(ride, rideStatus, userId);

        SosAlert alert = sosRepository.findByRide_IdAndStatus(ride.getId(), SosStatus.TRIGGERED)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (alert != null) {
            log.info("SOS already active for rideId={}, returning existing alertId={}", ride.getId(), alert.getId());
            alert.setLatitude(req.getLatitude());
            alert.setLongitude(req.getLongitude());
            alert.setLastPingAt(now);
            sosRepository.save(alert);

            sosLocationPingRepository.save(SosLocationPing.builder()
                    .sosAlert(alert)
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .build());
            return toResponse(alert, ride);
        }

        alert = SosAlert.builder()
                .ride(ride)
                .triggeredBy(triggeredBy)
                .triggeredByRole(role)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .status(SosStatus.TRIGGERED)
                .showedCall112Option(true)
                .triggeredAt(now)
                .lastPingAt(now)
                .build();

        alert = sosRepository.save(alert);

        rideStatus.setState(RideState.SOS_TRIGGERED);
        rideStatusRepository.save(rideStatus);

        sosLocationPingRepository.save(SosLocationPing.builder()
                .sosAlert(alert)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build());

        log.warn("SOS TRIGGERED — alertId={} rideId={} userId={} role={} lat={} lng={}",
                alert.getId(), ride.getId(), userId, role, req.getLatitude(), req.getLongitude());

        String trackingLink = buildTrackingLink(alert.getTrackingToken());
        String message = buildSosMessage(triggeredBy, ride, req.getLatitude(), req.getLongitude(), trackingLink);

        boolean counterpartNotified = notifyCounterpart(ride, rideStatus, userId, role, triggeredBy, alert.getId());
        alert.setCounterpartNotified(counterpartNotified);

        List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);
        notifyAdminsPush(admins, alert, ride, triggeredBy, role);
        boolean adminSmsSent = notifyAdminsSms(admins, message);
        alert.setAdminSmsSent(adminSmsSent);

        boolean contactSmsSent = notifyEmergencyContacts(triggeredBy, message);
        alert.setContactSmsSent(contactSmsSent);

        alert = sosRepository.save(alert);

        return toResponse(alert, ride);
    }

    @Transactional
    public void addLocationPing(Long alertId, SosLocationPingRequest req) {
        SosAlert alert = sosRepository.findById(alertId)
                .orElseThrow(() -> AppException.notFound("SOS alert not found"));

        if (alert.getStatus() != SosStatus.TRIGGERED) {
            return;
        }

        alert.setLatitude(req.getLatitude());
        alert.setLongitude(req.getLongitude());
        alert.setLastPingAt(LocalDateTime.now());
        sosRepository.save(alert);

        sosLocationPingRepository.save(SosLocationPing.builder()
                .sosAlert(alert)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build());
    }

    public List<SosLocationPingResponse> getLocationTrail(Long userId, Long alertId) {
        SosAlert alert = sosRepository.findById(alertId)
                .orElseThrow(() -> AppException.notFound("SOS alert not found"));

        assertCanViewAlert(userId, alert);

        return sosLocationPingRepository.findBySosAlert_IdOrderByRecordedAtAsc(alertId).stream()
                .map(this::toPingResponse)
                .collect(Collectors.toList());
    }

    public List<SosLocationPingResponse> getLocationTrailByToken(String trackingToken) {
        SosAlert alert = sosRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> AppException.notFound("SOS alert not found"));

        if (alert.getStatus() != SosStatus.TRIGGERED) {
            throw AppException.badRequest("Tracking no longer available — this SOS alert is closed");
        }

        return sosLocationPingRepository.findBySosAlert_IdOrderByRecordedAtAsc(alert.getId()).stream()
                .map(this::toPingResponse)
                .collect(Collectors.toList());
    }

    private void assertCanViewAlert(Long userId, SosAlert alert) {
        boolean isTriggerer = alert.getTriggeredBy().getId().equals(userId);
        boolean isAdmin = userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);

        if (!isTriggerer && !isAdmin) {
            throw AppException.forbidden("You are not authorized to view this SOS trail");
        }
    }

    private SosLocationPingResponse toPingResponse(SosLocationPing ping) {
        return SosLocationPingResponse.builder()
                .id(ping.getId())
                .latitude(ping.getLatitude())
                .longitude(ping.getLongitude())
                .recordedAt(ping.getRecordedAt())
                .build();
    }

    @Transactional
    public SosAlertResponse resolve(Long userId, Long alertId, boolean falseAlarm) {
        SosAlert alert = sosRepository.findById(alertId)
                .orElseThrow(() -> AppException.notFound("SOS alert not found"));

        if (alert.getStatus() != SosStatus.TRIGGERED) {
            throw AppException.badRequest("This SOS alert is already closed");
        }

        boolean isTriggerer = alert.getTriggeredBy().getId().equals(userId);

        User actingUser = isTriggerer
                ? alert.getTriggeredBy()
                : userRepository.findById(userId).orElseThrow(() -> AppException.notFound("User not found"));

        boolean isAdmin = actingUser.getRole() == Role.ADMIN;

        if (!isTriggerer && !isAdmin) {
            throw AppException.forbidden("Only the person who triggered SOS or an admin can resolve it");
        }

        applyResolution(alert, falseAlarm ? SosStatus.FALSE_ALARM : SosStatus.RESOLVED);

        return toResponse(alert, alert.getRide());
    }

    @Transactional
    public void autoResolve(SosAlert alert, SosStatus targetStatus) {
        applyResolution(alert, targetStatus);
    }

    private void applyResolution(SosAlert alert, SosStatus targetStatus) {
        alert.setStatus(targetStatus);
        alert.setResolvedAt(LocalDateTime.now());
        sosRepository.save(alert);

        rideStatusRepository.findByRide_Id(alert.getRide().getId()).ifPresent(rideStatus -> {
            if (rideStatus.getState() == RideState.SOS_TRIGGERED) {
                rideStatus.setState(RideState.STARTED);
                rideStatusRepository.save(rideStatus);
            }
        });

        if (targetStatus == SosStatus.FALSE_ALARM && alert.isContactSmsSent()) {
            String standDown = alert.getTriggeredBy().getFullName()
                    + " marked their earlier SOS as a FALSE ALARM. They are safe.";
            notifyEmergencyContacts(alert.getTriggeredBy(), standDown);
            notifyAdminsSms(userRepository.findByRoleAndActiveTrue(Role.ADMIN), standDown);
        }

        log.warn("SOS {} — alertId={}", targetStatus, alert.getId());
    }

    @Transactional
    public void resolveActiveSosForRide(Long rideId) {
        sosRepository.findByRide_IdAndStatus(rideId, SosStatus.TRIGGERED)
                .ifPresent(alert -> {
                    log.info("Ride {} completed with an active SOS (alertId={}) — auto-resolving", rideId, alert.getId());
                    autoResolve(alert, SosStatus.RESOLVED);
                });
    }

    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void expireStaleAlerts() {
        List<SosAlert> active = sosRepository.findByStatusOrderByTriggeredAtDesc(SosStatus.TRIGGERED);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRY_INACTIVITY_MINUTES);

        for (SosAlert alert : active) {
            LocalDateTime reference = alert.getLastPingAt() != null ? alert.getLastPingAt() : alert.getTriggeredAt();
            if (reference != null && reference.isBefore(cutoff)) {
                log.warn("SOS alertId={} had no ping for {}+ min — marking EXPIRED", alert.getId(), EXPIRY_INACTIVITY_MINUTES);
                autoResolve(alert, SosStatus.EXPIRED);
            }
        }
    }

    public List<SosActiveSummaryResponse> getActiveAlerts() {
        return sosRepository.findByStatusOrderByTriggeredAtDesc(SosStatus.TRIGGERED).stream()
                .map(a -> SosActiveSummaryResponse.builder()
                        .alertId(a.getId())
                        .rideId(a.getRide().getId())
                        .triggeredByName(a.getTriggeredBy().getFullName())
                        .triggeredByRole(a.getTriggeredByRole())
                        .status(a.getStatus())
                        .triggeredAt(a.getTriggeredAt())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveRole(Ride ride, RideStatus rideStatus, Long userId) {
        if (ride.getPostedBy().getId().equals(userId)) {
            return "DRIVER";
        }
        if (rideStatus.getBookedBy() != null && rideStatus.getBookedBy().getId().equals(userId)) {
            return "RIDER";
        }
        throw AppException.forbidden("You are not part of this ride");
    }

    private boolean notifyCounterpart(Ride ride, RideStatus rideStatus, Long triggeringUserId,
                                      String triggeringRole, User triggeredBy, Long alertId) {
        Long counterpartUserId = null;

        if ("RIDER".equals(triggeringRole)) {
            counterpartUserId = ride.getPostedBy().getId();
        } else if ("DRIVER".equals(triggeringRole) && rideStatus.getBookedBy() != null) {
            counterpartUserId = rideStatus.getBookedBy().getId();
        }

        if (counterpartUserId == null || counterpartUserId.equals(triggeringUserId)) {
            return false;
        }

        fcmService.sendToUser(
                counterpartUserId,
                "Emergency SOS triggered",
                triggeredBy.getFullName() + " has triggered an SOS on this ride. Please check on them immediately.",
                Map.of("type", "SOS_TRIGGERED", "sosAlertId", String.valueOf(alertId), "rideId", String.valueOf(ride.getId()))
        );
        return true;
    }

    private void notifyAdminsPush(List<User> admins, SosAlert alert, Ride ride, User triggeredBy, String role) {
        for (User admin : admins) {
            fcmService.sendToUser(
                    admin.getId(),
                    "SOS Alert",
                    triggeredBy.getFullName() + " (" + role + ") triggered SOS on ride #" + ride.getId(),
                    Map.of("type", "SOS_ALERT", "sosAlertId", String.valueOf(alert.getId()), "rideId", String.valueOf(ride.getId()))
            );
        }
    }

    private boolean notifyAdminsSms(List<User> admins, String message) {
        if (admins.isEmpty()) {
            log.warn("No active ADMIN users found — admin SMS not sent");
            return false;
        }
        boolean anySent = false;
        for (User admin : admins) {
            if (admin.getPhone() != null && !admin.getPhone().isBlank()) {
                anySent = smsClient.sendSms(admin.getPhone(), message) || anySent;
            }
        }
        return anySent;
    }

    private boolean notifyEmergencyContacts(User user, String message) {
        List<String> phones = new ArrayList<>();
        if (user.getEmergencyContact1Phone() != null) phones.add(user.getEmergencyContact1Phone());
        if (user.getEmergencyContact2Phone() != null) phones.add(user.getEmergencyContact2Phone());
        if (user.getEmergencyContact3Phone() != null) phones.add(user.getEmergencyContact3Phone());

        if (phones.isEmpty()) {
            log.warn("User {} has no emergency contacts configured — SOS SMS not sent", user.getId());
            return false;
        }

        boolean anySent = false;
        for (String phone : phones) {
            anySent = smsClient.sendSms(phone, message) || anySent;
        }
        return anySent;
    }

    private String buildTrackingLink(String trackingToken) {
        return "https://yourapp.example.com/sos/track/" + trackingToken;
    }

    private String buildSosMessage(User user, Ride ride, BigDecimal lat, BigDecimal lng, String trackingLink) {
        String driverName = ride.getPostedBy().getFullName();
        String driverPhone = ride.getPostedBy().getPhone();
        String vehicleNumber = ride.getVehicle() != null ? ride.getVehicle().getVehicleNumber() : "N/A";

        return "EMERGENCY: " + user.getFullName() + " pressed SOS during a ride (#" + ride.getId() + ").\n"
                + "Driver: " + driverName + " (" + driverPhone + "), Vehicle: " + vehicleNumber + "\n"
                + "Route: " + ride.getFromName() + " -> " + ride.getToName() + "\n"
                + "Current location: https://maps.google.com/?q=" + lat + "," + lng + "\n"
                + "Live tracking: " + trackingLink + "\n"
                + "Please respond immediately.";
    }

    private SosAlertResponse toResponse(SosAlert alert, Ride ride) {
        return SosAlertResponse.builder()
                .id(alert.getId())
                .trackingToken(alert.getTrackingToken())
                .rideId(ride.getId())
                .triggeredByUserId(alert.getTriggeredBy().getId())
                .triggeredByName(alert.getTriggeredBy().getFullName())
                .triggeredByRole(alert.getTriggeredByRole())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .status(alert.getStatus())
                .showedCall112Option(alert.isShowedCall112Option())
                .contactSmsSent(alert.isContactSmsSent())
                .adminSmsSent(alert.isAdminSmsSent())
                .counterpartNotified(alert.isCounterpartNotified())
                .triggeredAt(alert.getTriggeredAt())
                .resolvedAt(alert.getResolvedAt())
                .driverName(ride.getPostedBy().getFullName())
                .driverPhone(ride.getPostedBy().getPhone())
                .vehicleNumber(ride.getVehicle() != null ? ride.getVehicle().getVehicleNumber() : null)
                .fromName(ride.getFromName())
                .toName(ride.getToName())
                .build();
    }
}