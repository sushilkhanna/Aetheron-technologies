package com.bikepooling.controller;

import com.bikepooling.dto.request.*;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.AuthResponse;
import com.bikepooling.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest req) {

        authService.register(req);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "OTP sent to " + req.phone
                                + ". Please verify to complete registration.")
        );
    }


    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequest req) {

        AuthResponse auth = authService.verifyRegistrationOtp(req);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Phone verified! Account created successfully.", auth)
        );
    }


//    @PostMapping("/login/password")
//    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPassword(
//            @Valid @RequestBody LoginWithPasswordRequest req) {
//
//        AuthResponse auth = authService.loginWithPassword(req);
//        return ResponseEntity.ok(
//                ApiResponse.ok("Login successful", auth)
//        );
//    }


    @PostMapping("/login/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendLoginOtp(
            @Valid @RequestBody SendOtpRequest req) {

        authService.sendLoginOtp(req);
        return ResponseEntity.ok(
                ApiResponse.ok("OTP sent to " + req.phone)
        );
    }


    @PostMapping("/login/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithOtp(
            @Valid @RequestBody VerifyOtpRequest req) {

        AuthResponse auth = authService.loginWithOtp(req);
        return ResponseEntity.ok(
                ApiResponse.ok("Login successful", auth)
        );
    }


//    @PostMapping("/change-password")
//    public ResponseEntity<ApiResponse<Void>> changePassword(
//            @AuthenticationPrincipal Long userId,
//            @Valid @RequestBody ChangePasswordRequest req) {
//
//        authService.changePassword(userId, req);
//        return ResponseEntity.ok(
//                ApiResponse.ok("Password changed successfully.")
//        );
//    }
}
