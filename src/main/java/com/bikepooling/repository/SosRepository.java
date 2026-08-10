package com.bikepooling.repository;

import com.bikepooling.entity.SosAlert;
import com.bikepooling.enums.SosStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SosRepository extends JpaRepository<SosAlert, Long> {
    List<SosAlert> findByInstance_Id(Long instanceId);

    Optional<SosAlert> findByInstance_IdAndStatus(Long instanceId, SosStatus status);

    List<SosAlert> findByStatusOrderByTriggeredAtDesc(SosStatus status);

    Optional<SosAlert> findByTrackingToken(String trackingToken);
}