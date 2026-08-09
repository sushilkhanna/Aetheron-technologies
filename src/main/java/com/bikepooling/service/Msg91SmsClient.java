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

    public boolean sendSms(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            log.warn("Skipped SOS SMS — empty phone number");
            return false;
        }
        try {
            String normalizedPhone = normalize(phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", msg91Properties.getAuth().getKey());

            Map<String, Object> body = new HashMap<>();
            body.put("sender", msg91Properties.getSender().getId());
            body.put("route", "4");
            body.put("country", "91");
            body.put("sms", List.of(Map.of(
                    "message", message,
                    "to", List.of(normalizedPhone)
            )));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(MSG91_SEND_URL, request, String.class);

            log.info("SOS SMS sent to {}", mask(normalizedPhone));
            return true;
        } catch (Exception e) {
            log.error("Failed to send SOS SMS to {}: {}", mask(phone), e.getMessage());
            return false;
        }
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