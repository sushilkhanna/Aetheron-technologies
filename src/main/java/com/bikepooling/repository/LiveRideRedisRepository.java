package com.bikepooling.repository;

import com.bikepooling.dto.request.LiveRideLocation;
import com.bikepooling.dto.request.LiveRideMeta;
import com.bikepooling.dto.response.LiveRideSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

/**
 * Redis key layout:
 *
 *   live:meta:{rideId}   → LiveRideMeta JSON      TTL: 6h (safety net; deleted explicitly on terminal states)
 *   live:loc:{rideId}    → LiveRideLocation JSON  TTL: 5m (refreshed every ping — self-expires if driver goes dark)
 *   live:driver:{id}     → rideId                 TTL: 6h
 *   live:active-rides    → Set<rideId>            maintained index, used instead of KEYS scans
 *
 * Previous version used redisTemplate.keys("live:ride:*") to enumerate live
 * rides. KEYS is O(N) over the ENTIRE Redis keyspace (not just live rides)
 * and blocks the Redis event loop while it runs — this gets slower and more
 * expensive as your total key count grows, which is what "too many values"
 * was actually describing. findAllLive() below replaces it with SMEMBERS on
 * a maintained index + batched MGET — O(active rides), no blocking scan.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LiveRideRedisRepository {

    public static final Duration META_TTL     = Duration.ofHours(6);
    public static final Duration LOCATION_TTL = Duration.ofMinutes(5);

    private static final String META_PREFIX    = "live:meta:";
    private static final String LOC_PREFIX     = "live:loc:";
    private static final String DRIVER_PREFIX  = "live:driver:";
    private static final String ACTIVE_SET_KEY = "live:active-rides";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    // ── Meta ──────────────────────────────────────────────────────────────────

    public void saveMeta(LiveRideMeta meta) {
        try {
            String json = objectMapper.writeValueAsString(meta);
            redisTemplate.opsForValue().set(META_PREFIX + meta.getRideId(), json, META_TTL);
            redisTemplate.opsForValue().set(
                    DRIVER_PREFIX + meta.getDriverId(),
                    String.valueOf(meta.getRideId()),
                    META_TTL);
            redisTemplate.opsForSet().add(ACTIVE_SET_KEY, String.valueOf(meta.getRideId()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise LiveRideMeta rideId={}", meta.getRideId(), e);
        }
    }

    public LiveRideMeta findMeta(Long rideId) {
        return deserialise(redisTemplate.opsForValue().get(META_PREFIX + rideId), LiveRideMeta.class);
    }

    // ── Location ──────────────────────────────────────────────────────────────

    public void saveLocation(LiveRideLocation loc) {
        try {
            String json = objectMapper.writeValueAsString(loc);
            redisTemplate.opsForValue().set(LOC_PREFIX + loc.getRideId(), json, LOCATION_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise LiveRideLocation rideId={}", loc.getRideId(), e);
        }
    }

    public LiveRideLocation findLocation(Long rideId) {
        return deserialise(redisTemplate.opsForValue().get(LOC_PREFIX + rideId), LiveRideLocation.class);
    }

    // ── Driver index ──────────────────────────────────────────────────────────

    public Long findRideIdByDriverId(Long driverId) {
        String val = redisTemplate.opsForValue().get(DRIVER_PREFIX + driverId);
        return val == null ? null : Long.parseLong(val);
    }

    // ── Combined reads ────────────────────────────────────────────────────────

    public LiveRideSnapshot findSnapshot(Long rideId) {
        LiveRideMeta meta = findMeta(rideId);
        if (meta == null) return null;
        return LiveRideSnapshot.of(meta, findLocation(rideId));
    }

    /**
     * All currently-live rides, via SMEMBERS + batched MGET instead of KEYS.
     * Self-heals the index: if a rideId is in the set but its meta expired
     * (shouldn't normally happen given explicit delete(), but safety net),
     * it's dropped from the set here rather than returned as a broken entry.
     */
    public List<LiveRideSnapshot> findAllLive() {
        Set<String> rideIdStrs = redisTemplate.opsForSet().members(ACTIVE_SET_KEY);
        if (rideIdStrs == null || rideIdStrs.isEmpty()) return List.of();

        List<String> metaKeys = new ArrayList<>();
        List<String> locKeys  = new ArrayList<>();
        List<Long>   rideIds  = new ArrayList<>();
        for (String s : rideIdStrs) {
            Long id = Long.parseLong(s);
            rideIds.add(id);
            metaKeys.add(META_PREFIX + id);
            locKeys.add(LOC_PREFIX + id);
        }

        List<String> metaJsons = redisTemplate.opsForValue().multiGet(metaKeys);
        List<String> locJsons  = redisTemplate.opsForValue().multiGet(locKeys);

        List<LiveRideSnapshot> result = new ArrayList<>();
        List<String> staleRideIds = new ArrayList<>();

        for (int i = 0; i < rideIds.size(); i++) {
            LiveRideMeta meta = deserialise(
                    metaJsons != null ? metaJsons.get(i) : null, LiveRideMeta.class);

            if (meta == null) {
                staleRideIds.add(String.valueOf(rideIds.get(i)));
                continue;
            }

            LiveRideLocation loc = deserialise(
                    locJsons != null ? locJsons.get(i) : null, LiveRideLocation.class);

            result.add(LiveRideSnapshot.of(meta, loc));
        }

        if (!staleRideIds.isEmpty()) {
            redisTemplate.opsForSet().remove(ACTIVE_SET_KEY, staleRideIds.toArray());
            log.debug("Self-healed {} stale entries from active-rides index", staleRideIds.size());
        }

        return result;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(Long rideId, Long driverId) {
        redisTemplate.delete(META_PREFIX + rideId);
        redisTemplate.delete(LOC_PREFIX + rideId);
        redisTemplate.delete(DRIVER_PREFIX + driverId);
        redisTemplate.opsForSet().remove(ACTIVE_SET_KEY, String.valueOf(rideId));
        log.info("Live ride removed from Redis: rideId={} driverId={}", rideId, driverId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T deserialise(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialise {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }
}