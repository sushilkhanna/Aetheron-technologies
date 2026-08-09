package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.*;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ProfileResponse;
import com.bikepooling.service.AadhaarVerificationService;
import com.bikepooling.service.DrivingLicenceVerificationService;
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

    private final UserProfileService                userProfileService;
    private final AadhaarVerificationService        aadhaarVerificationService;
    private final DrivingLicenceVerificationService dlVerificationService;

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ProfileResponse>> completeProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteProfileRequest req) {

        ProfileResponse profile = userProfileService.completeProfile(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile completed successfully.", profile));
    }

    @PatchMapping("/update")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest req) {

        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully.",
                userProfileService.updateProfile(principal.getUserId(), req)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        ProfileResponse profile = userProfileService.getProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched successfully.", profile));
    }

    @PostMapping("/verify-aadhaar")
    public ResponseEntity<ApiResponse<Void>> verifyAadhaar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AadhaarVerificationRequest req) {

        aadhaarVerificationService.verifyAadhaar(principal.getUserId(), req.getAadhaarNumber());
        return ResponseEntity.ok(ApiResponse.ok("Aadhaar verified successfully.", null));
    }

    @PostMapping("/verify-dl")
    public ResponseEntity<ApiResponse<Void>> verifyDrivingLicence(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DrivingLicenceVerificationRequest req) {

        dlVerificationService.verifyDrivingLicence(
                principal.getUserId(), req.getDlNumber(), req.getDateOfBirth());
        return ResponseEntity.ok(ApiResponse.ok("Driving licence verified successfully.", null));
    }

    @PutMapping("/emergency-contacts")
    public ResponseEntity<ApiResponse<Void>> updateEmergencyContacts(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateEmergencyContactsRequest req) {

        userProfileService.updateEmergencyContacts(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Emergency contacts updated successfully.", null));
    }
}