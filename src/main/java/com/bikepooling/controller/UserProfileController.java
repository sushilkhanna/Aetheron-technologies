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
import java.util.Map;

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
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyAadhaar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AadhaarVerificationRequest req) {

        String status = aadhaarVerificationService.verifyAadhaar(principal.getUserId(), req.getAadhaarNumber(), null);
        String msg = "VERIFIED_BY_API".equalsIgnoreCase(status)
                ? "Aadhaar verified successfully via DigiLocker API."
                : "DigiLocker API key unavailable. Aadhaar verification request sent to Admin for manual review.";
        return ResponseEntity.ok(ApiResponse.ok(msg, Map.of("status", status)));
    }

    @PostMapping("/verify-dl")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyDrivingLicence(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DrivingLicenceVerificationRequest req) {

        String status = dlVerificationService.verifyDrivingLicence(
                principal.getUserId(), req.getDlNumber(), req.getDateOfBirth());
        String msg = "VERIFIED_BY_API".equalsIgnoreCase(status)
                ? "Driving licence verified successfully via API."
                : "DigiLocker API key unavailable. Driving Licence verification request sent to Admin for manual review.";
        return ResponseEntity.ok(ApiResponse.ok(msg, Map.of("status", status)));
    }

    @PutMapping("/emergency-contacts")
    public ResponseEntity<ApiResponse<Void>> updateEmergencyContacts(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateEmergencyContactsRequest req) {

        userProfileService.updateEmergencyContacts(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Emergency contacts updated successfully.", null));
    }
}