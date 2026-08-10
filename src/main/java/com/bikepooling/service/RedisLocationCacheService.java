package com.bikepooling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RedisLocationCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Fallback in-memory cache if Redis is temporarily unreachable
    private final Map<String, String> memoryFallback = new ConcurrentHashMap<>();

    private static final String SCHEDULED_RIDE_PREFIX = "live:scheduled_ride:";
    private static final String LIVE_RIDE_PREFIX = "live:live_ride:";
    private static final Duration KEY_TTL = Duration.ofMinutes(10);

    public RedisLocationCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisLocationSnapshot {
        private String rideType; // "SCHEDULED" or "LIVE"
        private Long id;         // instanceId or liveRideId
        private Long driverId;
        private double lat;
        private double lng;
        private Double bearingDegrees;
        private Double speedKmh;
        private long timestamp;
        private String state;
        private String updatedAt;
    }

    // ── Scheduled Ride Location in Redis ─────────────────────────────────────

    public void saveScheduledRideLocation(Long instanceId, Long driverId, double lat, double lng, Double bearing, Double speed, long timestamp, String state) {
        String key = SCHEDULED_RIDE_PREFIX + instanceId;
        RedisLocationSnapshot snapshot = RedisLocationSnapshot.builder()
                .rideType("SCHEDULED")
                .id(instanceId)
                .driverId(driverId)
                .lat(lat)
                .lng(lng)
                .bearingDegrees(bearing)
                .speedKmh(speed)
                .timestamp(timestamp > 0 ? timestamp : System.currentTimeMillis())
                .state(state)
                .updatedAt(LocalDateTime.now().toString())
                .build();

        saveToRedis(key, snapshot);
    }

    public RedisLocationSnapshot getScheduledRideLocation(Long instanceId) {
        String key = SCHEDULED_RIDE_PREFIX + instanceId;
        return getFromRedis(key);
    }

    public void removeScheduledRideLocation(Long instanceId) {
        String key = SCHEDULED_RIDE_PREFIX + instanceId;
        removeFromRedis(key);
    }

    // ── Live Ride Location in Redis ──────────────────────────────────────────

    public void saveLiveRideLocation(Long liveRideId, Long driverId, double lat, double lng, Double bearing, Double speed, long timestamp, String state) {
        String key = LIVE_RIDE_PREFIX + liveRideId;
        RedisLocationSnapshot snapshot = RedisLocationSnapshot.builder()
                .rideType("LIVE")
                .id(liveRideId)
                .driverId(driverId)
                .lat(lat)
                .lng(lng)
                .bearingDegrees(bearing)
                .speedKmh(speed)
                .timestamp(timestamp > 0 ? timestamp : System.currentTimeMillis())
                .state(state)
                .updatedAt(LocalDateTime.now().toString())
                .build();

        saveToRedis(key, snapshot);
    }

    public RedisLocationSnapshot getLiveRideLocation(Long liveRideId) {
        String key = LIVE_RIDE_PREFIX + liveRideId;
        return getFromRedis(key);
    }

    public void removeLiveRideLocation(Long liveRideId) {
        String key = LIVE_RIDE_PREFIX + liveRideId;
        removeFromRedis(key);
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    private void saveToRedis(String key, RedisLocationSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            try {
                redisTemplate.opsForValue().set(key, json, KEY_TTL);
                log.debug("Saved location to Redis: key={}", key);
            } catch (Exception e) {
                log.warn("Redis write failed for key={}, fallback to memory: {}", key, e.getMessage());
                memoryFallback.put(key, json);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize location snapshot: {}", e.getMessage());
        }
    }

    private RedisLocationSnapshot getFromRedis(String key) {
        String json = null;
        try {
            json = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis read failed for key={}, checking memory fallback: {}", key, e.getMessage());
        }

        if (json == null) {
            json = memoryFallback.get(key);
        }

        if (json == null) return null;

        try {
            return objectMapper.readValue(json, RedisLocationSnapshot.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize location snapshot from key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void removeFromRedis(String key) {
        memoryFallback.remove(key);
        try {
            redisTemplate.delete(key);
            log.debug("Removed location from Redis: key={}", key);
        } catch (Exception e) {
            log.warn("Redis delete failed for key={}: {}", key, e.getMessage());
        }
    }
}
