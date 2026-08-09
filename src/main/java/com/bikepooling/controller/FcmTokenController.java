package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.RegisterFcmTokenRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.entity.FcmToken;
import com.bikepooling.entity.User;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.FcmTokenRepository;
import com.bikepooling.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenRepository fcmTokenRepo;
    private final UserRepository     userRepo;

   @PostMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @Valid @RequestBody RegisterFcmTokenRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepo.findById(principal.getUserId())
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (req.getDeviceId() != null) {

            FcmToken token = fcmTokenRepo
                    .findByDeviceId(req.getDeviceId())
                    .orElse(null);

            if (token != null) {
                token.setUser(user);
                token.setToken(req.getToken());

            } else {

                long count = fcmTokenRepo.countByUserId(user.getId());

                if (count >= 10) {

                    fcmTokenRepo
                            .findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                            .ifPresent(fcmTokenRepo::delete);
                }

                token = FcmToken.builder()
                        .user(user)
                        .deviceId(req.getDeviceId())
                        .token(req.getToken())
                        .build();
            }

            fcmTokenRepo.save(token);
        }

        return ResponseEntity.ok(ApiResponse.ok("FCM token registered.", null));
    }

    @DeleteMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> removeToken(
            @RequestParam String token
    ) {
        fcmTokenRepo.deleteByToken(token);
        return ResponseEntity.ok(ApiResponse.ok("FCM token removed.", null));
    }
}