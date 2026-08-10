package com.bikepooling.controller;

import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.DriverKycDTO;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.config.UserPrincipal;
import com.bikepooling.service.AdminKycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final AdminKycService adminKycService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<DriverKycDTO>>> getKycRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String type,
            @RequestParam(required = false)    String search) {

        return ResponseEntity.ok(ApiResponse.ok("KYC requests fetched",
                adminKycService.getKycRequests(page, size, status, type, search)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKycStats() {
        return ResponseEntity.ok(ApiResponse.ok("KYC stats fetched",
                adminKycService.getKycStats()));
    }

    @PostMapping("/{kycId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverKycDTO>> approveKyc(
            @PathVariable Long kycId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long adminId = principal != null ? principal.getUserId() : null;
        return ResponseEntity.ok(ApiResponse.ok("KYC request approved successfully",
                adminKycService.approveKyc(kycId, adminId)));
    }

    @PostMapping("/{kycId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverKycDTO>> rejectKyc(
            @PathVariable Long kycId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long adminId = principal != null ? principal.getUserId() : null;
        String reason = body != null ? body.get("reason") : "Rejected by Admin";
        return ResponseEntity.ok(ApiResponse.ok("KYC request rejected",
                adminKycService.rejectKyc(kycId, adminId, reason)));
    }
}
