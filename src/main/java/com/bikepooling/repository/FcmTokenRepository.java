package com.bikepooling.repository;

import com.bikepooling.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // all tokens for a user (one user may have multiple devices)
    @Query("SELECT f FROM FcmToken f WHERE f.user.id = :userId")
    List<FcmToken> findByUserId(@Param("userId") Long userId);

    // find by token string (to update when token rotates)
    Optional<FcmToken> findByToken(String token);

//    // find by device id for upsert
//    @Query("SELECT f FROM FcmToken f WHERE f.user.id = :userId AND f.deviceId = :deviceId")
//    Optional<FcmToken> findByUserIdAndDeviceId(
//            @Param("userId")   Long userId,
//            @Param("deviceId") String deviceId
//    );

    Optional<FcmToken> findByDeviceId(String deviceId);

    long countByUserId(Long userId);

    Optional<FcmToken> findFirstByUserIdOrderByCreatedAtAsc(Long userId);

    // remove stale token (called when FCM reports token invalid)
    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.token = :token")
    void deleteByToken(@Param("token") String token);
}