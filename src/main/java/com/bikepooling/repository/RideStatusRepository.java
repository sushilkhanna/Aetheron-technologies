package com.bikepooling.repository;

import com.bikepooling.entity.RideStatus;
import com.bikepooling.enums.RideState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideStatusRepository extends JpaRepository<RideStatus, Long> {

    @Query("""
        SELECT COUNT(rs) FROM RideStatus rs
        WHERE rs.state IN :states
        AND rs.ride.deleted = false
        """)
    long countByStateIn(@Param("states") List<RideState> states);

    @Query("SELECT rs FROM RideStatus rs WHERE rs.ride.id = :rideId")
    Optional<RideStatus> findByRideId(@Param("rideId") Long rideId);

    @Query("""
        SELECT rs FROM RideStatus rs
        JOIN FETCH rs.ride r
        JOIN FETCH r.postedBy
        JOIN FETCH r.vehicle
        WHERE r.id = :rideId
        """)
    Optional<RideStatus> findByRideIdWithDetails(@Param("rideId") Long rideId);

    /**
     * Rides in the pre-departure window that haven't been notified yet.
     * preDepartureNotifiedAt IS NULL guarantees exactly one notification
     * per ride per state — fixes the "fires every 5 min" bug.
     */
    @Query("""
        SELECT rs FROM RideStatus rs
        JOIN FETCH rs.ride r
        JOIN FETCH r.postedBy
        LEFT JOIN FETCH rs.bookedBy
        WHERE rs.state = :state
          AND r.departAt BETWEEN :windowStart AND :windowEnd
          AND rs.preDepartureNotifiedAt IS NULL
          AND r.deleted = false
        """)
    List<RideStatus> findRidesInWindowWithStateNotYetNotified(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd")   LocalDateTime windowEnd,
            @Param("state")       RideState state
    );

    /**
     * OPEN rides whose departAt is at/before the given threshold.
     * Caller passes (now - 15min) as the threshold to apply the grace period.
     */
    @Query("""
        SELECT rs.ride.id FROM RideStatus rs
        WHERE rs.state = com.bikepooling.enums.RideState.OPEN
          AND rs.ride.departAt <= :threshold
          AND rs.ride.deleted = false
        """)
    List<Long> findExpiredOpenRideIds(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("""
        UPDATE RideStatus rs
        SET rs.state = :expiredState
        WHERE rs.ride.id IN :rideIds
        AND rs.state = com.bikepooling.enums.RideState.OPEN
        """)
    void markRidesExpired(
            @Param("rideIds")      List<Long> rideIds,
            @Param("expiredState") RideState expiredState
    );

    List<RideStatus> findByRideIdIn(List<Long> rideIds);

    Optional<RideStatus> findByRide_Id(Long rideId);

    @Query("""
    select rs
    from RideStatus rs
    join fetch rs.ride r
    left join fetch rs.bookedBy b
    where rs.state in :states
    and (
        r.postedBy.id = :userId
        or b.id = :userId
    )
""")
    List<RideStatus> findActiveRidesForUser(
            @Param("userId") Long userId,
            @Param("states") List<RideState> states
    );

    @Query("""
        SELECT rs FROM RideStatus rs
        JOIN FETCH rs.ride r
        JOIN FETCH r.postedBy
        LEFT JOIN FETCH rs.bookedBy
        WHERE rs.state IN :states
        AND r.deleted = false
        """)
    List<RideStatus> findByStateIn(@Param("states") List<RideState> states);

    @Query("""
    SELECT rs FROM RideStatus rs
    JOIN FETCH rs.ride r
    JOIN FETCH r.postedBy
    LEFT JOIN FETCH rs.bookedBy
    WHERE (:state IS NULL OR rs.state = :state)
      AND (:driverId IS NULL OR r.postedBy.id = :driverId)
      AND (:from IS NULL OR r.departAt >= :from)
      AND (:to IS NULL OR r.departAt <= :to)
      AND r.deleted = false
    ORDER BY r.departAt DESC
""")
    Page<RideStatus> findForAdmin(
            @Param("state")    RideState state,
            @Param("driverId") Long driverId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            Pageable pageable
    );

    // For KPI cards — count by state for today
    @Query("""
    SELECT rs.state, COUNT(rs)
    FROM RideStatus rs
    JOIN rs.ride r
    WHERE r.departAt >= :dayStart AND r.departAt < :dayEnd
      AND r.deleted = false
    GROUP BY rs.state
""")
    List<Object[]> countByStateForDay(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd
    );

    // For the graph — last N days
    @Query("""
    SELECT CAST(r.departAt AS date), rs.state, COUNT(rs)
    FROM RideStatus rs
    JOIN rs.ride r
    WHERE r.departAt >= :from AND r.deleted = false
    GROUP BY CAST(r.departAt AS date), rs.state
    ORDER BY CAST(r.departAt AS date) ASC
""")
    List<Object[]> dailyStatsByState(@Param("from") LocalDateTime from);

    // Revenue — sum fare from completed rides
    @Query("""
    SELECT COALESCE(SUM(ra.bookerFare), 0)
    FROM RideApplication ra
    JOIN ra.ride r
    JOIN RideStatus rs ON rs.ride = r
    WHERE rs.state = 'COMPLETED'
      AND rs.completedAt >= :from
      AND rs.completedAt < :to
""")
    BigDecimal sumRevenueForPeriod(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    @Query("""
    SELECT rs FROM RideStatus rs
    JOIN FETCH rs.ride r
    JOIN FETCH r.postedBy d
    JOIN FETCH r.vehicle v
    LEFT JOIN FETCH rs.bookedBy b
    WHERE (:state IS NULL OR rs.state = :state)
      AND (:driverId IS NULL OR d.id = :driverId)
      AND (:from IS NULL OR r.departAt >= :from)
      AND (:to IS NULL OR r.departAt <= :to)
      AND (
            :keyword IS NULL
            OR LOWER(d.fullName) LIKE :keyword
            OR LOWER(b.fullName) LIKE :keyword
            OR LOWER(v.vehicleNumber) LIKE :keyword
            OR CAST(r.id AS string) LIKE :keyword
          )
      AND r.deleted = false
    ORDER BY r.departAt DESC
""")
    Page<RideStatus> searchForAdmin(
            @Param("state")    RideState state,
            @Param("driverId") Long driverId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            @Param("keyword")  String keyword,
            Pageable pageable
    );
}