package com.bikepooling.service;

import com.bikepooling.config.Msg91Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain (non-OTP-template) transactional SMS via MSG91.
 * Swap this out for your real OTP sender's client if you have one —
 * SosService only needs a sendSms(phone, message) method.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Msg91SmsClient {

    private static final String MSG91_SEND_URL = "https://api.msg91.com/api/v2/sendsms";

    private final Msg91Properties msg91Properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean sendSms(String phone, String message, String context) {
        String logPrefix = (context != null && !context.isBlank()) ? "[" + context + " - SMS]" : "[SMS]";

        if (phone == null || phone.isBlank()) {
            log.warn("{} Skipped SMS dispatch — Phone number is empty", logPrefix);
            return false;
        }

        String authKey = (msg91Properties != null && msg91Properties.getAuth() != null) ? msg91Properties.getAuth().getKey() : null;
        if (authKey == null || authKey.isBlank() || "YOUR_MSG91_AUTH_KEY".equalsIgnoreCase(authKey)) {
            log.error("{} SMS DISPATCH FAILED — MSG91 Auth Key is missing or unconfigured! Cannot send SMS to {}",
                    logPrefix, mask(phone));
            return false;
        }

        try {
            String normalizedPhone = normalize(phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            String senderId = (msg91Properties.getSender() != null && msg91Properties.getSender().getId() != null)
                    ? msg91Properties.getSender().getId() : "";

            Map<String, Object> body = new HashMap<>();
            body.put("sender", senderId);
            body.put("route", "4");
            body.put("country", "91");
            body.put("sms", List.of(Map.of(
                    "message", message,
                    "to", List.of(normalizedPhone)
            )));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(MSG91_SEND_URL, request, String.class);

            log.info("{} SMS sent successfully to {}", logPrefix, mask(normalizedPhone));
            return true;
        } catch (Exception e) {
            log.error("{} SMS DISPATCH FAILED for recipient {}: {}", logPrefix, mask(phone), e.getMessage());
            return false;
        }
    }

    public boolean sendSms(String phone, String message) {
        return sendSms(phone, message, "GENERAL");
    }

    private String normalize(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() == 10 ? "91" + digits : digits;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}