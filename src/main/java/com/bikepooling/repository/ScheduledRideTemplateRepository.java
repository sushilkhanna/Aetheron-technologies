package com.bikepooling.repository;

import com.bikepooling.entity.ScheduledRideTemplate;
import com.bikepooling.enums.ScheduledRideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduledRideTemplateRepository extends JpaRepository<ScheduledRideTemplate, Long> {

    @Query("""
        SELECT COUNT(t) FROM ScheduledRideTemplate t
        WHERE t.postedBy.id = :driverId
          AND t.status = com.bikepooling.enums.ScheduledRideStatus.ACTIVE
          AND t.deleted = false
        """)
    long countActiveByDriver(@Param("driverId") Long driverId);

    @Query("""
        SELECT t FROM ScheduledRideTemplate t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE t.id = :id AND t.deleted = false
        """)
    Optional<ScheduledRideTemplate> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT t FROM ScheduledRideTemplate t
        JOIN FETCH t.vehicle
        WHERE t.postedBy.id = :driverId
          AND t.status = :status
          AND t.deleted = false
        ORDER BY t.createdAt DESC
        """)
    List<ScheduledRideTemplate> findByDriverAndStatus(
            @Param("driverId") Long driverId,
            @Param("status") ScheduledRideStatus status);
}