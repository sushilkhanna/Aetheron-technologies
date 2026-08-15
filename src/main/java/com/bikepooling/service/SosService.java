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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${sos.tracking.base-url:https://bikepooling.in/sos/track}")
    private String trackingBaseUrl;

    private final SosRepository sosRepository;
    private final SosLocationPingRepository sosLocationPingRepository;
    private final ScheduledRideInstanceRepository instanceRepository;
    private final UserRepository userRepository;
    private final Msg91SmsClient smsClient;
    private final FcmService fcmService;

    @Transactional
    public SosAlertResponse trigger(Long userId, SosTriggerRequest req) {
        ScheduledRideInstance instance = instanceRepository.findByIdWithDetails(req.getInstanceId())
                .orElseThrow(() -> AppException.notFound("Scheduled ride instance not found"));

        User triggeredBy = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        String role = resolveRole(instance, userId);

        SosAlert alert = sosRepository.findByInstance_IdAndStatus(instance.getId(), SosStatus.TRIGGERED)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (alert != null) {
            log.info("SOS already active for instanceId={}, returning existing alertId={}", instance.getId(), alert.getId());
            alert.setLatitude(req.getLatitude());
            alert.setLongitude(req.getLongitude());
            alert.setLastPingAt(now);
            sosRepository.save(alert);

            sosLocationPingRepository.save(SosLocationPing.builder()
                    .sosAlert(alert)
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .build());
            return toResponse(alert, instance);
        }

        alert = SosAlert.builder()
                .instance(instance)
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

        instance.setState(RideState.SOS_TRIGGERED);
        instanceRepository.save(instance);

        sosLocationPingRepository.save(SosLocationPing.builder()
                .sosAlert(alert)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build());

        log.warn("SOS TRIGGERED — alertId={} instanceId={} userId={} role={} lat={} lng={}",
                alert.getId(), instance.getId(), userId, role, req.getLatitude(), req.getLongitude());

        String trackingLink = buildTrackingLink(alert.getTrackingToken());
        String message = buildSosMessage(triggeredBy, instance, req.getLatitude(), req.getLongitude(), trackingLink);

        boolean counterpartNotified = notifyCounterpart(instance, userId, role, triggeredBy, alert.getId());
        alert.setCounterpartNotified(counterpartNotified);

        List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);
        notifyAdminsPush(admins, alert, instance, triggeredBy, role);
        boolean adminSmsSent = notifyAdminsSms(admins, message);
        alert.setAdminSmsSent(adminSmsSent);

        boolean contactSmsSent = notifyEmergencyContacts(triggeredBy, message);
        alert.setContactSmsSent(contactSmsSent);

        alert = sosRepository.save(alert);

        return toResponse(alert, instance);
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

        return toResponse(alert, alert.getInstance());
    }

    @Transactional
    public void autoResolve(SosAlert alert, SosStatus targetStatus) {
        applyResolution(alert, targetStatus);
    }

    private void applyResolution(SosAlert alert, SosStatus targetStatus) {
        alert.setStatus(targetStatus);
        alert.setResolvedAt(LocalDateTime.now());
        sosRepository.save(alert);

        ScheduledRideInstance instance = alert.getInstance();
        if (instance != null && instance.getState() == RideState.SOS_TRIGGERED) {
            instance.setState(RideState.STARTED);
            instanceRepository.save(instance);
        }

        if (targetStatus == SosStatus.FALSE_ALARM && alert.isContactSmsSent()) {
            String standDown = alert.getTriggeredBy().getFullName()
                    + " marked their earlier SOS as a FALSE ALARM. They are safe.";
            notifyEmergencyContacts(alert.getTriggeredBy(), standDown);
            notifyAdminsSms(userRepository.findByRoleAndActiveTrue(Role.ADMIN), standDown);
        }

        log.warn("SOS {} — alertId={}", targetStatus, alert.getId());
    }

    @Transactional
    public void resolveActiveSosForInstance(Long instanceId) {
        sosRepository.findByInstance_IdAndStatus(instanceId, SosStatus.TRIGGERED)
                .ifPresent(alert -> {
                    log.info("Instance {} completed with an active SOS (alertId={}) — auto-resolving", instanceId, alert.getId());
                    autoResolve(alert, SosStatus.RESOLVED);
                });
    }

    @Scheduled(fixedRate = 300000)
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
                        .instanceId(a.getInstance().getId())
                        .triggeredByName(a.getTriggeredBy().getFullName())
                        .triggeredByRole(a.getTriggeredByRole())
                        .status(a.getStatus())
                        .triggeredAt(a.getTriggeredAt())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveRole(ScheduledRideInstance instance, Long userId) {
        if (instance.getTemplate().getPostedBy().getId().equals(userId)) {
            return "DRIVER";
        }
        if (instance.getBookedBy() != null && instance.getBookedBy().getId().equals(userId)) {
            return "RIDER";
        }
        throw AppException.forbidden("You are not part of this ride");
    }

    private boolean notifyCounterpart(ScheduledRideInstance instance, Long triggeringUserId,
                                       String triggeringRole, User triggeredBy, Long alertId) {
        Long counterpartUserId = null;

        if ("RIDER".equals(triggeringRole)) {
            counterpartUserId = instance.getTemplate().getPostedBy().getId();
        } else if ("DRIVER".equals(triggeringRole) && instance.getBookedBy() != null) {
            counterpartUserId = instance.getBookedBy().getId();
        }

        if (counterpartUserId == null || counterpartUserId.equals(triggeringUserId)) {
            return false;
        }

        fcmService.sendToUser(
                counterpartUserId,
                "Emergency SOS triggered",
                triggeredBy.getFullName() + " has triggered an SOS on this ride. Please check on them immediately.",
                Map.of("type", "SOS_TRIGGERED", "sosAlertId", String.valueOf(alertId), "instanceId", String.valueOf(instance.getId()))
        );
        return true;
    }

    private void notifyAdminsPush(List<User> admins, SosAlert alert, ScheduledRideInstance instance, User triggeredBy, String role) {
        for (User admin : admins) {
            fcmService.sendToUser(
                    admin.getId(),
                    "SOS Alert",
                    triggeredBy.getFullName() + " (" + role + ") triggered SOS on scheduled ride instance #" + instance.getId(),
                    Map.of("type", "SOS_ALERT", "sosAlertId", String.valueOf(alert.getId()), "instanceId", String.valueOf(instance.getId()))
            );
        }
    }

    private boolean notifyAdminsSms(List<User> admins, String message) {
        if (admins.isEmpty()) {
            log.warn("[SOS EMERGENCY - SMS] No active ADMIN users found — admin SMS not sent");
            return false;
        }
        boolean anySent = false;
        for (User admin : admins) {
            if (admin.getPhone() != null && !admin.getPhone().isBlank()) {
                anySent = smsClient.sendSms(admin.getPhone(), message, "SOS EMERGENCY - ADMIN") || anySent;
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
            log.warn("[SOS EMERGENCY - SMS] User {} has no emergency contacts configured — SOS SMS not sent", user.getId());
            return false;
        }

        boolean anySent = false;
        for (String phone : phones) {
            anySent = smsClient.sendSms(phone, message, "SOS EMERGENCY - CONTACT") || anySent;
        }
        return anySent;
    }

    private String buildTrackingLink(String trackingToken) {
        String base = trackingBaseUrl.endsWith("/") ? trackingBaseUrl.substring(0, trackingBaseUrl.length() - 1) : trackingBaseUrl;
        return base + "/" + trackingToken;
    }

    private String buildSosMessage(User user, ScheduledRideInstance instance, BigDecimal lat, BigDecimal lng, String trackingLink) {
        ScheduledRideTemplate template = instance.getTemplate();
        String driverName = template.getPostedBy().getFullName();
        String driverPhone = template.getPostedBy().getPhone();
        String vehicleNumber = template.getVehicle() != null ? template.getVehicle().getVehicleNumber() : "N/A";

        return "EMERGENCY: " + user.getFullName() + " pressed SOS during a ride (#" + instance.getId() + ").\n"
                + "Driver: " + driverName + " (" + driverPhone + "), Vehicle: " + vehicleNumber + "\n"
                + "Route: " + template.getFromName() + " -> " + template.getToName() + "\n"
                + "Current location: https://maps.google.com/?q=" + lat + "," + lng + "\n"
                + "Live tracking: " + trackingLink + "\n"
                + "Please respond immediately.";
    }

    private SosAlertResponse toResponse(SosAlert alert, ScheduledRideInstance instance) {
        ScheduledRideTemplate template = instance.getTemplate();
        return SosAlertResponse.builder()
                .id(alert.getId())
                .trackingToken(alert.getTrackingToken())
                .instanceId(instance.getId())
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
                .driverName(template.getPostedBy().getFullName())
                .driverPhone(template.getPostedBy().getPhone())
                .vehicleNumber(template.getVehicle() != null ? template.getVehicle().getVehicleNumber() : null)
                .fromName(template.getFromName())
                .toName(template.getToName())
                .build();
    }
}