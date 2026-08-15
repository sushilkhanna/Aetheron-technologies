package com.bikepooling.controller;

import com.bikepooling.dto.request.SendComingSoonSmsRequest;
import com.bikepooling.dto.response.AdminMessageResponse;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ComingSoonUserDTO;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.service.ComingSoonUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coming-soon")
@RequiredArgsConstructor
public class AdminComingSoonController {

    private final ComingSoonUserService comingSoonUserService;

    @GetMapping("/subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ComingSoonUserDTO>>> getSubscribers(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(required = false)           String search,
            @RequestParam(required = false)           Boolean notified,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {

        PagedResponse<ComingSoonUserDTO> data = comingSoonUserService.getSubscribers(page, size, search, notified, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok("Subscribers fetched successfully", data));
    }

    @PostMapping("/send-sms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminMessageResponse>> sendSms(
            @RequestBody @Valid SendComingSoonSmsRequest request) {

        ApiResponse<AdminMessageResponse> response = comingSoonUserService.sendSmsToSubscribers(request);
        return ResponseEntity.ok(response);
    }
}
