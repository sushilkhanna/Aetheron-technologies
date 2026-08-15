package com.bikepooling.service;

import com.bikepooling.dto.request.*;
import com.bikepooling.dto.response.AuthResponse;
import com.bikepooling.entity.User;
import com.bikepooling.enums.Role;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.UserRepository;
import com.bikepooling.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository      userRepository;
    private final JwtUtil             jwtUtil;
    private final OtpService          otpService;
    private final StringRedisTemplate redisTemplate;
    private final AdminMetricsService metricsService;

    @Value("${jwt.expiry.ms}")
    private long jwtExpiryMs;

    private static final String PRE_REGISTER_PREFIX = "pre_register:";


    public void register(RegisterRequest req) {
        req.phone = req.phone.trim();
        req.fullName = req.fullName.trim();
        if (userRepository.existsByPhone(req.phone)) {
            throw AppException.conflict("Phone number is already registered.");
        }
        redisTemplate.opsForValue().set(
                PRE_REGISTER_PREFIX + req.phone,
                req.fullName,
                Duration.ofMinutes(3)
        );
        otpService.sendOtp(req.phone);
    }

    public AuthResponse verifyRegistrationOtp(VerifyOtpRequest req) {

        boolean valid = otpService.verifyOtp(req.phone, req.otp);
        if (!valid) {
            throw AppException.badRequest("Invalid or expired OTP. Please request a new one.");
        }

        String key      = PRE_REGISTER_PREFIX + req.phone;
        String fullName = redisTemplate.opsForValue().get(key);

        if (fullName == null) {
            throw AppException.badRequest("Registration session expired. Please register again.");
        }

        if (userRepository.existsByPhone(req.phone)) {
            throw AppException.conflict("Phone already registered. Please login instead.");
        }

        User user = User.builder()
                .fullName(fullName)
                .phone(req.phone)
                .role(Role.GUEST)
                .active(true)
                .phoneVerified(true)
                .build();

        userRepository.save(user);
        redisTemplate.delete(key);
        metricsService.onUserRegistered();
        log.info("User registered via OTP: {}", req.phone);
        return buildAuthResponse(user);
    }

    public void sendLoginOtp(SendOtpRequest req) {
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> AppException.notFound(
                        "No account found with this phone number."));

        checkAccountActive(user);
        otpService.sendOtp(req.phone);
    }

    public AuthResponse loginWithOtp(VerifyOtpRequest req) {
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> AppException.notFound(
                        "No account found with this phone number."));

        checkAccountActive(user);

        boolean valid = otpService.verifyOtp(req.phone, req.otp);
        if (!valid) {
            throw AppException.badRequest(
                    "Invalid or expired OTP. Please request a new one.");
        }

        return buildAuthResponse(user);
    }

    public void sendAdminLoginOtp(SendOtpRequest req) {
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> AppException.notFound(
                        "No account found with this phone number."));

        // Reject non-admins BEFORE sending the OTP
        if (user.getRole() != Role.ADMIN) {
            log.warn("Admin OTP requested for non-admin phone: {}", req.phone);
            throw AppException.notFound("No account found with this phone number.");
        }

        checkAccountActive(user);
        otpService.sendOtp(req.phone);
        log.info("Admin OTP sent to: {}", req.phone);
    }

    public AuthResponse adminLoginWithOtp(VerifyOtpRequest req) {
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> AppException.notFound(
                        "No account found with this phone number."));

        checkAccountActive(user);

        boolean valid = otpService.verifyOtp(req.phone, req.otp);
        if (!valid) {
            throw AppException.badRequest("Invalid or expired OTP. Please request a new one.");
        }

        if (user.getRole() != Role.ADMIN) {
            log.warn("Admin login attempted by non-admin after OTP verify: {}", req.phone);
            throw AppException.forbidden("Access denied.");
        }

        log.info("Admin logged in: {}", req.phone);
        return buildAuthResponse(user);
    }

    private void checkAccountActive(User user) {
        if (!user.isActive()) {
            throw AppException.forbidden("Your account has been deactivated. Contact support.");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(user))
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .phoneVerified(user.isPhoneVerified())
                .build();
    }
}
