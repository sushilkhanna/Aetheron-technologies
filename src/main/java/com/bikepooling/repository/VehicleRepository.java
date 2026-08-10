package com.bikepooling.repository;

import com.bikepooling.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumberAndDeletedAtIsNull(String vehicleNumber);

    Optional<Vehicle> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Vehicle> findByUserIdAndActiveTrue(Long userId);

    boolean existsByUserIdAndDeletedAtIsNull(Long userId);
}