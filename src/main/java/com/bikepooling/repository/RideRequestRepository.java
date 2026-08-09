package com.bikepooling.repository;

import com.bikepooling.entity.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {

    @Query("""
        SELECT COUNT(r) > 0 FROM RideRequest r
        WHERE r.booker.id = :bookerId
          AND r.active = true
          AND r.expiresAt > CURRENT_TIMESTAMP
        """)
    boolean hasActiveRequest(@Param("bookerId") Long bookerId);

    @Query("""
        SELECT r FROM RideRequest r
        JOIN FETCH r.booker
        WHERE r.active = true
          AND r.expiresAt > :now
          AND r.departFrom <= :departAt
          AND r.departTo   >= :departAt
        """)
    List<RideRequest> findActiveRequestsForWindow(
            @Param("now")      LocalDateTime now,
            @Param("departAt") LocalDateTime departAt);

    @Modifying
    @Query("""
        UPDATE RideRequest r
        SET r.active = false
        WHERE r.active = true
          AND r.expiresAt <= :now
        """)
    int deactivateExpiredRequests(@Param("now") LocalDateTime now);
}