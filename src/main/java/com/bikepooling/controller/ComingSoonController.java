package com.bikepooling.controller;

import com.bikepooling.dto.request.ComingSoonRegisterRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ComingSoonUserDTO;
import com.bikepooling.service.ComingSoonUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/coming-soon")
@RequiredArgsConstructor
public class ComingSoonController {

    private final ComingSoonUserService comingSoonUserService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ComingSoonUserDTO>> registerPhone(
            @RequestBody @Valid ComingSoonRegisterRequest request,
            HttpServletRequest httpServletRequest) {

        String ipAddress = getClientIp(httpServletRequest);
        ApiResponse<ComingSoonUserDTO> response = comingSoonUserService.registerPhone(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
