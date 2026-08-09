package com.bikepooling.repository;

import com.bikepooling.entity.ScheduledRideApplication;
import com.bikepooling.entity.ScheduledRideApplicationDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduledRideApplicationRepository extends JpaRepository<ScheduledRideApplication, Long> {

    @Query("""
        SELECT a FROM ScheduledRideApplication a
        JOIN FETCH a.booker
        JOIN FETCH a.template
        WHERE a.id = :id
        """)
    Optional<ScheduledRideApplication> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT COUNT(a) > 0 FROM ScheduledRideApplication a
        WHERE a.template.id = :templateId
          AND a.booker.id = :bookerId
        """)
    boolean existsByTemplateIdAndBookerId(
            @Param("templateId") Long templateId,
            @Param("bookerId") Long bookerId);

    /** Check if userA is driver and userB is booker, OR userB is driver and userA is booker. */
    @Query("""
        SELECT COUNT(a) > 0 FROM ScheduledRideApplication a
        WHERE (a.template.postedBy.id = :userA AND a.booker.id = :userB)
           OR (a.template.postedBy.id = :userB AND a.booker.id = :userA)
        """)
    boolean existsBookingBetweenUsers(
            @Param("userA") Long userA,
            @Param("userB") Long userB);

    @Query("select d from ScheduledRideApplicationDay d " +
            "join fetch d.application a join fetch a.booker " +
            "join fetch d.instance i join fetch i.template t join fetch t.postedBy " +
            "where d.id in :ids and d.deleted = false")
    List<ScheduledRideApplicationDay> findActiveByIdIn(@Param("ids") List<Long> ids);

    @Query("select d from ScheduledRideApplicationDay d " +
            "join fetch d.application a join fetch a.booker " +
            "join fetch d.instance i " +
            "where i.template.id = :templateId and d.deleted = false")
    List<ScheduledRideApplicationDay> findActiveByTemplateId(@Param("templateId") Long templateId);
}
