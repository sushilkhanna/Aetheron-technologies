package com.bikepooling.service;

import com.bikepooling.dto.request.VehicleRequest;
import com.bikepooling.dto.response.VehicleResponse;
import com.bikepooling.entity.User;
import com.bikepooling.entity.Vehicle;
import com.bikepooling.enums.Role;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.UserRepository;
import com.bikepooling.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleResponse addVehicle(Long userId, VehicleRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if(user.getRole() != Role.DRIVER){
            throw AppException.conflict("First verify your DL");
        }

        boolean alreadyHasVehicle = vehicleRepository
                .existsByUserIdAndDeletedAtIsNull(userId);
        if (alreadyHasVehicle) {
            throw AppException.conflict("You already have an active vehicle. Remove it before adding a new one.");
        }

        if (vehicleRepository.existsByVehicleNumberAndDeletedAtIsNull(req.getVehicleNumber())) {
            throw AppException.conflict("Vehicle number is already registered");
        }

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(req.getVehicleNumber().toUpperCase())
                .user(user)
                .active(true)
                .build();

        vehicleRepository.save(vehicle);
        return toResponse(vehicle);
    }

    public void removeVehicle(Long userId) {
        Vehicle vehicle = vehicleRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> AppException.notFound("No active vehicle found"));

        vehicle.setActive(false);
        vehicle.setDeletedAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }

    public VehicleResponse getMyVehicle(Long userId) {
        return vehicleRepository.findByUserIdAndDeletedAtIsNull(userId)
                .map(this::toResponse)
                .orElseThrow(() -> AppException.notFound("No active vehicle found"));
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vehicleNumber(vehicle.getVehicleNumber())
                .active(vehicle.isActive())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}