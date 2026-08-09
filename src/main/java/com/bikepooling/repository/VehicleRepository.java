package com.bikepooling.repository;

import com.bikepooling.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumberAndDeletedAtIsNull(String vehicleNumber);

    Optional<Vehicle> findByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByUserIdAndDeletedAtIsNull(Long userId);

}