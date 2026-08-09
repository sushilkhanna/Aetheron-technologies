package com.bikepooling.repository;

import com.bikepooling.entity.Ride;
import com.bikepooling.enums.RideState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    @Query("""
        SELECT COUNT(r) > 0 FROM Ride r
        JOIN RideStatus rs ON rs.ride = r
        WHERE r.vehicle.id = :vehicleId
          AND rs.state IN :states
          AND r.deleted = false
        """)
    boolean existsActiveRideForVehicle(@Param("vehicleId") Long vehicleId,
                                       @Param("states") List<RideState> states);

    /**
     * Count active rides for a driver — used to enforce MAX_ACTIVE_RIDES cap.
     */
    @Query("""
        SELECT COUNT(r) FROM Ride r
        JOIN RideStatus rs ON rs.ride = r
        WHERE r.postedBy.id = :driverId
          AND rs.state IN :states
          AND r.deleted = false
        """)
    long countActiveRidesForDriver(@Param("driverId") Long driverId,
                                   @Param("states") List<RideState> states);

    @Query("""
        SELECT r FROM Ride r
        JOIN FETCH r.postedBy
        JOIN FETCH r.vehicle
        WHERE r.id = :rideId
          AND r.deleted = false
        """)
    Optional<Ride> findRideWithDetails(@Param("rideId") Long rideId);

    /**
     * Driver's own rides — paginated, newest first.
     */
    @Query(value = """
        SELECT r FROM Ride r
        JOIN FETCH r.vehicle
        WHERE r.postedBy.id = :driverId
          AND r.deleted = false
        ORDER BY r.departAt DESC
        """,
            countQuery = """
        SELECT COUNT(r) FROM Ride r
        WHERE r.postedBy.id = :driverId
          AND r.deleted = false
        """)
    Page<Ride> findDriverRidesPaged(@Param("driverId") Long driverId,
                                    Pageable pageable);

    /**
     * Open rides in the booker's time window, excluding their own rides.
     * Paginated — geo filter applied in-memory after this.
     */
    @Query(value = """
        SELECT r FROM Ride r
        JOIN FETCH r.postedBy
        JOIN FETCH r.vehicle
        JOIN RideStatus rs ON rs.ride = r
        WHERE rs.state = 'OPEN'
          AND r.departAt BETWEEN :from AND :to
          AND r.postedBy.id <> :excludeUserId
          AND r.deleted = false
        ORDER BY r.departAt ASC
        """,
            countQuery = """
        SELECT COUNT(r) FROM Ride r
        JOIN RideStatus rs ON rs.ride = r
        WHERE rs.state = 'OPEN'
          AND r.departAt BETWEEN :from AND :to
          AND r.postedBy.id <> :excludeUserId
          AND r.deleted = false
        """)
    Page<Ride> findOpenRidesInWindowPaged(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludeUserId") Long excludeUserId,
            Pageable pageable);

    @Query("""
    SELECT COUNT(r) FROM Ride r
    WHERE r.deleted = false
    AND FUNCTION('DATE', r.departAt) = CURRENT_DATE
    """)
    long countRidesToday();

    @Query("""
    SELECT COALESCE(SUM(r.fare), 0) FROM Ride r
    JOIN RideStatus rs ON rs.ride = r
    WHERE r.deleted = false
    AND rs.state = :completedState
    AND FUNCTION('DATE', rs.completedAt) = CURRENT_DATE
    """)
    BigDecimal sumRevenueToday(@Param("completedState") RideState completedState);

    /**
     * Same filter as findOpenRidesInWindowPaged but unpaginated —
     * used for FCM matching where we need every candidate, not one page.
     * Result sets here are expected to be small (open rides in a time window).
     */
    @Query("""
        SELECT r FROM Ride r
        JOIN FETCH r.postedBy
        JOIN RideStatus rs ON rs.ride = r
        WHERE rs.state = 'OPEN'
          AND r.departAt BETWEEN :from AND :to
          AND r.postedBy.id <> :excludeUserId
          AND r.deleted = false
        """)
    List<Ride> findOpenRidesInWindow(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludeUserId") Long excludeUserId
    );


}