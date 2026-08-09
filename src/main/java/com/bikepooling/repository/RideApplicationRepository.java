package com.bikepooling.repository;

import com.bikepooling.entity.RideApplication;
import com.bikepooling.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RideApplicationRepository extends JpaRepository<RideApplication, Long> {

    @Query("""
        SELECT COUNT(a) > 0 FROM RideApplication a
        WHERE a.ride.id = :rideId
          AND a.booker.id = :bookerId
          AND a.status IN :statuses
          AND a.deleted = false
        """)
    boolean existsByRideAndBooker(@Param("rideId") Long rideId,
                                  @Param("bookerId") Long bookerId,
                                  @Param("statuses") List<ApplicationStatus> statuses);

    @Query("""
        SELECT COUNT(a) > 0 FROM RideApplication a
        WHERE a.booker.id = :bookerId
          AND a.status = 'FINISH'
          AND a.deleted = false
        """)
    boolean hasFinishBooking(@Param("bookerId") Long bookerId);

    @Query("""
        SELECT a FROM RideApplication a
        JOIN FETCH a.booker
        WHERE a.ride.id = :rideId
          AND a.status = 'PENDING'
          AND a.deleted = false
        """)
    List<RideApplication> findPendingByRideId(@Param("rideId") Long rideId);

    @Query("""
        SELECT a FROM RideApplication a
        JOIN FETCH a.ride
        JOIN FETCH a.booker
        WHERE a.id = :id
          AND a.deleted = false
        """)
    Optional<RideApplication> findActiveById(@Param("id") Long id);

    @Modifying
    @Query("""
    UPDATE RideApplication a
        SET a.status = 'WITHDRAWN',
            a.deleted = true,
            a.deletedAt = :now
        WHERE a.booker.id = :bookerId
          AND a.status = 'PENDING'
          AND a.ride.id <> :excludeRideId
          AND a.deleted = false
    """)
    int withdrawOtherPendingApplications(
            @Param("bookerId") Long bookerId,
            @Param("excludeRideId") Long excludeRideId,
            @Param("now") LocalDateTime now);

    @Query("""
        SELECT a FROM RideApplication a
        JOIN FETCH a.ride r
        WHERE a.ride.id = :rideId
          AND a.status = 'CONFIRMED'
          AND a.deleted = false
        """)
    Optional<RideApplication> findConfirmedByRideId(@Param("rideId") Long rideId);

    @Query(value = """
        SELECT a FROM RideApplication a
        JOIN FETCH a.ride r
        JOIN FETCH r.postedBy
        WHERE a.booker.id = :bookerId
          AND a.status IN :statuses
          AND (:from IS NULL OR a.createdAt >= :from)
          AND (:to   IS NULL OR a.createdAt <= :to)
          AND a.deleted = false
        ORDER BY a.createdAt DESC
        """,
            countQuery = """
        SELECT COUNT(a) FROM RideApplication a
        WHERE a.booker.id = :bookerId
          AND a.status IN :statuses
          AND (:from IS NULL OR a.createdAt >= :from)
          AND (:to   IS NULL OR a.createdAt <= :to)
          AND a.deleted = false
        """)
    Page<RideApplication> findByBookerAndStatusesAndDateRange(
            @Param("bookerId") Long bookerId,
            @Param("statuses") List<ApplicationStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Modifying
    @Query("""
        UPDATE RideApplication a
        SET a.status = 'EXPIRED', a.deletedAt = :now
        WHERE a.ride.id = :rideId
          AND a.status = 'PENDING'
          AND a.deleted = false
        """)
    void expireApplicationsForRide(@Param("rideId") Long rideId,
                                   @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE RideApplication a
        SET a.status = 'REJECTED',
            a.deletedAt = :now
        WHERE a.ride.id = :rideId
          AND a.status = 'PENDING'
          AND a.deleted = false
          AND a.id <> :confirmedApplicationId
        """)
    void rejectOtherApplicants(
            @Param("rideId") Long rideId,
            @Param("confirmedApplicationId") Long confirmedApplicationId,
            @Param("now") LocalDateTime now);

    @Query("""
    SELECT a.booker.id
    FROM RideApplication a
    WHERE a.ride.id = :rideId
      AND a.status = 'PENDING'
      AND a.deleted = false
      AND a.id <> :applicationId
""")
    List<Long> findPendingBookerIds(
            @Param("rideId") Long rideId,
            @Param("applicationId") Long applicationId);
}