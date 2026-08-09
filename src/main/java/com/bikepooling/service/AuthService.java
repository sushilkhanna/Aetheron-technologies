package com.bikepooling.service;

import com.bikepooling.dto.request.*;
import com.bikepooling.dto.response.AuthResponse;
import com.bikepooling.entity.User;
import com.bikepooling.enums.Role;
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

    @Value("${jwt.expiry.ms}")
    private long jwtExpiryMs;

    private static final String PRE_REGISTER_PREFIX = "pre_register:";


    public void register(RegisterRequest req) {
        req.phone = req.phone.trim();
        req.fullName = req.fullName.trim();
        if (userRepository.existsByPhone(req.phone)) {
            throw new RuntimeException("Phone number is already registered.");
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
            throw new RuntimeException("Invalid or expired OTP. Please request a new one.");
        }

        String key      = PRE_REGISTER_PREFIX + req.phone;
        String fullName = redisTemplate.opsForValue().get(key);

        if (fullName == null) {
            throw new RuntimeException("Registration session expired. Please register again.");
        }

        if (userRepository.existsByPhone(req.phone)) {
            throw new RuntimeException("Phone already registered. Please login instead.");
        }

        User user = User.builder()
                .fullName(fullName)
                .phone(req.phone)
                .role(Role.USER)
                .active(true)
                .phoneVerified(true)
                .build();

        userRepository.save(user);
        redisTemplate.delete(key);

        log.info("User registered via OTP: {}", req.phone);
        return buildAuthResponse(user);
    }

//    public AuthResponse loginWithPassword(LoginWithPasswordRequest req) {
//        User user = userRepository.findByPhone(req.phone)
//                .orElseThrow(() -> new RuntimeException(
//                        "No account found with this phone number."));
//
//        checkAccountActive(user);
//
//        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
//            throw new RuntimeException("Incorrect password.");
//        }
//
//        return buildAuthResponse(user);
//    }

    public void sendLoginOtp(SendOtpRequest req) {
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> new RuntimeException(
                        "No account found with this phone number."));

        checkAccountActive(user);
        otpService.sendOtp(req.phone);
    }

    public AuthResponse loginWithOtp(VerifyOtpRequest req) {
        User user = userRepository.findByPhone(req.phone)
                .orElseThrow(() -> new RuntimeException(
                        "No account found with this phone number."));

        checkAccountActive(user);

        boolean valid = otpService.verifyOtp(req.phone, req.otp);
        if (!valid) {
            throw new RuntimeException(
                    "Invalid or expired OTP. Please request a new one.");
        }

        return buildAuthResponse(user);
    }

//    public void changePassword(Long userId, ChangePasswordRequest req) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found."));
//
//        checkAccountActive(user);
//
//        user.setPasswordHash(passwordEncoder.encode(req.newPassword));
//        userRepository.save(user);
//
//        String blacklistKey = "blacklist:user:" + userId;
//        redisTemplate.opsForValue().set(blacklistKey, "true", Duration.ofMillis(jwtExpiryMs));
//
//        log.info("Password changed for userId: {}", userId);
//    }

    private void checkAccountActive(User user) {
        if (!user.isActive()) {
            throw new RuntimeException("Your account has been deactivated. Contact support.");
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
