package com.bikepooling.controller;

import com.bikepooling.dto.request.CompleteProfileRequest;
import com.bikepooling.dto.request.UpdateProfileRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ProfileResponse;
import com.bikepooling.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ProfileResponse>> completeProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CompleteProfileRequest req) {

        ProfileResponse profile = userProfileService.completeProfile(userId, req);
        return ResponseEntity.ok(
                ApiResponse.ok("Profile completed successfully.", profile)
        );
    }

    @PatchMapping("/update")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest req) {

        return ResponseEntity.ok(
                ApiResponse.ok("Profile updated successfully.",
                        userProfileService.updateProfile(userId, req))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal Long userId) {

        ProfileResponse profile = userProfileService.getProfile(userId);
        return ResponseEntity.ok(
                ApiResponse.ok("Profile fetched successfully.", profile)
        );
    }
}