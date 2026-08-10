package com.bikepooling.repository;

import com.bikepooling.entity.DriverKycRequest;
import com.bikepooling.enums.KycStatus;
import com.bikepooling.enums.KycType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverKycRequestRepository extends JpaRepository<DriverKycRequest, Long> {

    Optional<DriverKycRequest> findByUserIdAndKycType(Long userId, KycType kycType);

    List<DriverKycRequest> findByUserId(Long userId);

    @Query("""
        SELECT k FROM DriverKycRequest k
        JOIN FETCH k.user u
        WHERE (:status IS NULL OR k.status = :status)
          AND (:type IS NULL OR k.kycType = :type)
          AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(k.documentNumber) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY k.submittedAt DESC
        """)
    Page<DriverKycRequest> searchKycRequests(
            @Param("status") KycStatus status,
            @Param("type") KycType type,
            @Param("search") String search,
            Pageable pageable);

    long countByStatus(KycStatus status);

    long countByKycTypeAndStatus(KycType type, KycStatus status);
}
