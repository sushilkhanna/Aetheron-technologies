package com.bikepooling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;

    @Value("${otp.expiry.seconds}")
    private long otpExpirySeconds;

    @Value("${msg91.auth.key}")
    private String msg91AuthKey;

    @Value("${msg91.template.id}")
    private String msg91TemplateId;

    @Value("${msg91.sender.id}")
    private String msg91SenderId;

    private static final String OTP_PREFIX      = "otp:";

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://control.msg91.com")
            .build();

    public void sendOtp(String phone) {
        checkRateLimit(phone);
        String otp = generateOtp(4);
        redisTemplate.opsForValue().set(
                OTP_PREFIX + phone, otp,
                Duration.ofSeconds(otpExpirySeconds));
        sendSmsViaMSG91(phone, otp);
    }

    public boolean verifyOtp(String phone, String submitted) {
        checkAttemptLimit(phone);
        String key    = OTP_PREFIX + phone;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(submitted)) {
            String attemptKey = "otp:attempt:" + phone;
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);
            if(attempts == 1) {
                redisTemplate.expire(attemptKey, Duration.ofMinutes(15));
            }
            return false;
        }
        redisTemplate.delete(key);
        redisTemplate.delete("otp:attempt:" + phone);
        redisTemplate.delete("otp:rate:" + phone);
        return true;
    }

    private void sendSmsViaMSG91(String phone, String otp) {
        String body = """
                {
                    "template_id": "%s",
                    "sender":      "%s",
                    "short_url":   "0",
                    "mobiles":     "91%s",
                    "VAR1":        "%s"
                }
                """.formatted(msg91TemplateId, msg91SenderId,
                phone, otp);
        callMSG91(body);
    }

    private void callMSG91(String body) {
        try {
            String response = webClient.post()
                    .uri("/api/v5/flow")
                    .header("authkey", msg91AuthKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            resp -> resp.bodyToMono(String.class)
                                    .map(err -> new RuntimeException("MSG91 failed: " + err)))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("MSG91 response: {}", response);
        } catch (Exception e) {
            log.error("MSG91 call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateOtp(int digits) {
        SecureRandom r   = SECURE_RANDOM;
        int          min = (int) Math.pow(10, digits - 1);
        int          max = (int) Math.pow(10, digits) - 1;
        return String.valueOf(min + r.nextInt(max - min + 1));
    }

    private void checkRateLimit(String phone) {
        String key = "otp:rate:" + phone;

        Long count = redisTemplate.opsForValue().increment(key);
        if(count==1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (count != null && count > 5) {
            throw new RuntimeException("Too many OTP requests. Please try after 10 minutes.");
        }
    }

    private void checkAttemptLimit(String phone) {
        String key = "otp:attempt:" + phone;
        String val = redisTemplate.opsForValue().get(key);
        int attempts = val == null ? 0 : Integer.parseInt(val);
        if (attempts > 10) {
            throw new RuntimeException("Too many wrong attempts. Try again later.");
        }
    }
}