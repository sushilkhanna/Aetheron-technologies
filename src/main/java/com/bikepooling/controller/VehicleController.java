package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.VehicleRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.VehicleResponse;
import com.bikepooling.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> addVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VehicleRequest req) {

        VehicleResponse vehicle = vehicleService.addVehicle(principal.getUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle added successfully.", vehicle));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeVehicle(
            @AuthenticationPrincipal UserPrincipal principal) {

        vehicleService.removeVehicle(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Vehicle removed successfully.", null));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<VehicleResponse>> getMyVehicle(
            @AuthenticationPrincipal UserPrincipal principal) {

        VehicleResponse vehicle = vehicleService.getMyVehicle(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Vehicle fetched successfully.", vehicle));
    }
}