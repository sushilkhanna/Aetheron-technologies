package com.bikepooling.repository;

import com.bikepooling.entity.ScheduledRideInstance;
import com.bikepooling.enums.RideState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ScheduledRideInstanceRepository extends JpaRepository<ScheduledRideInstance, Long> {

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE i.id = :id
        """)
    Optional<ScheduledRideInstance> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE i.id = :id
        """)
    Optional<ScheduledRideInstance> findByIdForUpdate(@Param("id") Long id);

    Optional<ScheduledRideInstance> findByTemplateIdAndRideDate(Long templateId, LocalDate rideDate);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        WHERE i.template.id = :templateId
        ORDER BY i.rideDate ASC
        """)
    List<ScheduledRideInstance> findByTemplateId(@Param("templateId") Long templateId);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        WHERE i.template.id = :templateId
          AND i.state IN :states
        ORDER BY i.rideDate ASC
        """)
    List<ScheduledRideInstance> findByTemplateIdAndStateIn(
            @Param("templateId") Long templateId,
            @Param("states") List<RideState> states);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE i.state = com.bikepooling.enums.RideState.OPEN
          AND (
                (:date IS NOT NULL AND i.rideDate = :date)
                OR (:date IS NULL AND i.dayOfWeek IN :wantedDays)
              )
          AND i.departTime BETWEEN :windowFrom AND :windowTo
          AND i.rideDate >= CURRENT_DATE
          AND t.postedBy.id <> :excludeUserId
          AND t.deleted = false
        ORDER BY i.rideDate ASC, i.departTime ASC
        """)
    List<ScheduledRideInstance> searchOpenInstances(
            @Param("date") LocalDate date,
            @Param("wantedDays") Set<DayOfWeek> wantedDays,
            @Param("windowFrom") LocalTime windowFrom,
            @Param("windowTo") LocalTime windowTo,
            @Param("excludeUserId") Long excludeUserId);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE i.state = com.bikepooling.enums.RideState.OPEN
          AND i.rideDate IN :dates
          AND i.departTime BETWEEN :windowFrom AND :windowTo
          AND i.rideDate >= CURRENT_DATE
          AND t.postedBy.id <> :excludeUserId
          AND t.deleted = false
        ORDER BY i.rideDate ASC, i.departTime ASC
        """)
    List<ScheduledRideInstance> searchOpenInstancesByDates(
            @Param("dates") Set<LocalDate> dates,
            @Param("windowFrom") LocalTime windowFrom,
            @Param("windowTo") LocalTime windowTo,
            @Param("excludeUserId") Long excludeUserId);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE i.state = com.bikepooling.enums.RideState.OPEN
          AND i.rideDate = :date
          AND i.departTime BETWEEN :windowFrom AND :windowTo
          AND i.rideDate >= CURRENT_DATE
          AND t.postedBy.id <> :excludeUserId
          AND t.deleted = false
        ORDER BY i.rideDate ASC, i.departTime ASC
        """)
    List<ScheduledRideInstance> searchOpenInstancesByDate(
            @Param("date") LocalDate date,
            @Param("windowFrom") LocalTime windowFrom,
            @Param("windowTo") LocalTime windowTo,
            @Param("excludeUserId") Long excludeUserId);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE i.state = com.bikepooling.enums.RideState.OPEN
          AND i.dayOfWeek IN :wantedDays
          AND i.departTime BETWEEN :windowFrom AND :windowTo
          AND i.rideDate >= CURRENT_DATE
          AND t.postedBy.id <> :excludeUserId
          AND t.deleted = false
        ORDER BY i.rideDate ASC, i.departTime ASC
        """)
    List<ScheduledRideInstance> searchOpenInstancesByDays(
            @Param("wantedDays") Set<DayOfWeek> wantedDays,
            @Param("windowFrom") LocalTime windowFrom,
            @Param("windowTo") LocalTime windowTo,
            @Param("excludeUserId") Long excludeUserId);

    @Query("""
        SELECT i FROM ScheduledRideInstance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        JOIN FETCH t.vehicle
        WHERE i.state = com.bikepooling.enums.RideState.OPEN
          AND i.departTime BETWEEN :windowFrom AND :windowTo
          AND i.rideDate >= CURRENT_DATE
          AND t.postedBy.id <> :excludeUserId
          AND t.deleted = false
        ORDER BY i.rideDate ASC, i.departTime ASC
        """)
    List<ScheduledRideInstance> searchOpenInstancesAll(
            @Param("windowFrom") LocalTime windowFrom,
            @Param("windowTo") LocalTime windowTo,
            @Param("excludeUserId") Long excludeUserId);

    @Modifying
    @Query("""
        UPDATE ScheduledRideInstance i
        SET i.state = com.bikepooling.enums.RideState.CANCELLED, i.cancelledAt = :now
        WHERE i.template.id = :templateId
          AND i.dayOfWeek IN :removedDays
          AND i.state = com.bikepooling.enums.RideState.OPEN
        """)
    void cancelOpenInstancesForDays(
            @Param("templateId") Long templateId,
            @Param("removedDays") Set<DayOfWeek> removedDays,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE ScheduledRideInstance i
        SET i.departTime = :departTime, i.extraDistanceKm = :extraDistanceKm
        WHERE i.template.id = :templateId
          AND i.state = com.bikepooling.enums.RideState.OPEN
        """)
    void refreshOpenInstances(
            @Param("templateId") Long templateId,
            @Param("departTime") LocalTime departTime,
            @Param("extraDistanceKm") BigDecimal extraDistanceKm);
}