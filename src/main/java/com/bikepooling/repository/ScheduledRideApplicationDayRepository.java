package com.bikepooling.repository;

import com.bikepooling.entity.ScheduledRideApplicationDay;
import com.bikepooling.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduledRideApplicationDayRepository extends JpaRepository<ScheduledRideApplicationDay, Long> {

    @Query("""
        SELECT d FROM ScheduledRideApplicationDay d
        JOIN FETCH d.application a
        JOIN FETCH a.booker
        JOIN FETCH d.instance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        WHERE d.id = :id AND d.deleted = false
        """)
    Optional<ScheduledRideApplicationDay> findActiveById(@Param("id") Long id);

    @Query("""
        SELECT d FROM ScheduledRideApplicationDay d
        JOIN FETCH d.application a
        JOIN FETCH a.booker
        JOIN FETCH d.instance i
        JOIN FETCH i.template t
        JOIN FETCH t.postedBy
        WHERE d.id IN :ids AND d.deleted = false
        """)
    List<ScheduledRideApplicationDay> findActiveByIdIn(@Param("ids") List<Long> ids);

    /** All PENDING day-links for one instance — used for cascade-cancel on confirm. */
    @Query("""
        SELECT d FROM ScheduledRideApplicationDay d
        JOIN FETCH d.application a
        JOIN FETCH a.booker
        WHERE d.instance.id = :instanceId
          AND d.status = com.bikepooling.enums.ApplicationStatus.PENDING
          AND d.deleted = false
        """)
    List<ScheduledRideApplicationDay> findPendingByInstanceId(@Param("instanceId") Long instanceId);

    /** Driver's view: all day-applications across a template. */
    @Query("""
        SELECT d FROM ScheduledRideApplicationDay d
        JOIN FETCH d.application a
        JOIN FETCH a.booker
        JOIN FETCH d.instance i
        WHERE i.template.id = :templateId
          AND d.deleted = false
        ORDER BY i.rideDate ASC
        """)
    List<ScheduledRideApplicationDay> findActiveByTemplateId(@Param("templateId") Long templateId);

    /** Booker's view: all day-applications created by booker across templates. */
    @Query("""
        SELECT d FROM ScheduledRideApplicationDay d
        JOIN FETCH d.application a
        JOIN FETCH a.booker
        JOIN FETCH a.template t
        JOIN FETCH t.postedBy
        JOIN FETCH d.instance i
        WHERE a.booker.id = :bookerId
          AND d.deleted = false
        ORDER BY i.rideDate ASC
        """)
    List<ScheduledRideApplicationDay> findActiveByBookerId(@Param("bookerId") Long bookerId);

    /** Every non-terminal link belonging to one application — used to decide if the whole application is now dead. */
    @Query("""
        SELECT COUNT(d) FROM ScheduledRideApplicationDay d
        WHERE d.application.id = :applicationId
          AND d.status IN :statuses
          AND d.deleted = false
        """)
    long countByApplicationIdAndStatusIn(
            @Param("applicationId") Long applicationId,
            @Param("statuses") List<ApplicationStatus> statuses);

    /**
     * Checks if userA and userB currently have a present ride booked or active application.
     */
    @Query("""
        SELECT COUNT(d) > 0 FROM ScheduledRideApplicationDay d
        WHERE d.deleted = false
          AND d.status IN (com.bikepooling.enums.ApplicationStatus.CONFIRMED, com.bikepooling.enums.ApplicationStatus.PENDING)
          AND ((d.instance.template.postedBy.id = :userA AND d.application.booker.id = :userB)
            OR (d.instance.template.postedBy.id = :userB AND d.application.booker.id = :userA))
        """)
    boolean hasPresentRideBooked(
            @Param("userA") Long userA,
            @Param("userB") Long userB);
}
