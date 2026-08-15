package com.bikepooling.repository;

import com.bikepooling.entity.ComingSoonUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ComingSoonUserRepository extends JpaRepository<ComingSoonUser, Long>, JpaSpecificationExecutor<ComingSoonUser> {
    boolean existsByPhone(String phone);
    Optional<ComingSoonUser> findByPhone(String phone);

    @Query("SELECT c FROM ComingSoonUser c WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.platform) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:notified IS NULL OR c.notified = :notified)")
    Page<ComingSoonUser> searchSubscribers(
            @Param("search") String search,
            @Param("notified") Boolean notified,
            Pageable pageable
    );
}
