package com.bikepooling.repository;

import com.bikepooling.entity.LiveRide;
import com.bikepooling.enums.LiveRideState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LiveRideRepository extends JpaRepository<LiveRide, Long> {

    @Query("""
        SELECT r FROM LiveRide r
        JOIN FETCH r.driver d
        LEFT JOIN FETCH r.booker b
        LEFT JOIN FETCH r.vehicle v
        WHERE r.id = :id
        """)
    Optional<LiveRide> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT r FROM LiveRide r
        WHERE r.driver.id = :driverId AND r.state IN :states
        ORDER BY r.createdAt DESC
        """)
    List<LiveRide> findActiveByDriverId(@Param("driverId") Long driverId, @Param("states") List<LiveRideState> states);

    @Query("""
        SELECT r FROM LiveRide r
        WHERE r.booker.id = :bookerId AND r.state IN :states
        ORDER BY r.createdAt DESC
        """)
    List<LiveRide> findActiveByBookerId(@Param("bookerId") Long bookerId, @Param("states") List<LiveRideState> states);

    @Query("""
        SELECT r FROM LiveRide r
        WHERE (r.driver.id = :userId OR r.booker.id = :userId) AND r.state IN :states
        ORDER BY r.createdAt DESC
        """)
    List<LiveRide> findActiveByUserId(@Param("userId") Long userId, @Param("states") List<LiveRideState> states);

    @Query("""
        SELECT r FROM LiveRide r
        JOIN FETCH r.driver d
        LEFT JOIN FETCH r.booker b
        LEFT JOIN FETCH r.vehicle v
        WHERE r.state IN (com.bikepooling.enums.LiveRideState.LIVE, com.bikepooling.enums.LiveRideState.CONFIRMED, com.bikepooling.enums.LiveRideState.VERIFIED)
        """)
    List<LiveRide> findAllActiveLiveRides();

    @Query("SELECT COUNT(r) FROM LiveRide r WHERE r.state IN :states")
    long countByStateIn(@Param("states") List<LiveRideState> states);
}
