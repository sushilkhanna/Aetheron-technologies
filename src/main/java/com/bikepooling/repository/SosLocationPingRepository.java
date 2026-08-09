package com.bikepooling.repository;

import com.bikepooling.entity.SosLocationPing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SosLocationPingRepository extends JpaRepository<SosLocationPing, Long> {
    List<SosLocationPing> findBySosAlert_IdOrderByRecordedAtAsc(Long sosAlertId);
}