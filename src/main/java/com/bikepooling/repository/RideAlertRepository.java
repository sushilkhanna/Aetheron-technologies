package com.bikepooling.repository;

import com.bikepooling.entity.RideAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideAlertRepository extends JpaRepository<RideAlert, Long> {

    // find active alerts whose time window overlaps the ride's departAt
    // and whose source/destination are within radius of the ride
    // radius matching is done in Java after this fetch (small result set)
    @Query("""
        SELECT a FROM RideAlert a
        JOIN FETCH a.user
        WHERE a.active = true
        AND a.expiresAt > :now
        AND a.windowFrom <= :departAt
        AND a.windowTo   >= :departAt
        """)
    List<RideAlert> findActiveAlertsForTime(
            @Param("now")       LocalDateTime now,
            @Param("departAt")  LocalDateTime departAt
    );

    // all active alerts by a user
    @Query("""
        SELECT a FROM RideAlert a
        WHERE a.user.id = :userId
        AND a.active = true
        AND a.expiresAt > :now
        ORDER BY a.createdAt DESC
        """)
    List<RideAlert> findActiveByUserId(
            @Param("userId") Long userId,
            @Param("now")    LocalDateTime now
    );

    // deactivate expired alerts (called by scheduler)
    @Modifying
    @Query("""
        UPDATE RideAlert a
        SET a.active = false
        WHERE a.expiresAt <= :now
        AND a.active = true
        """)
    int deactivateExpiredAlerts(@Param("now") LocalDateTime now);
}