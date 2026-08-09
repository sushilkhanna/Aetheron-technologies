package com.bikepooling.repository;

import com.bikepooling.dto.request.ActiveLiveSearch;
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
 *   live:search:{bookerId}  → JSON of ActiveLiveSearch   TTL: 3 min
 *
 * TTL is set once at creation — no refresh.
 * When TTL expires the search is naturally over.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActiveLiveSearchRepository {

    public static final Duration SEARCH_TTL = Duration.ofMinutes(3);

    private static final String PREFIX = "live:search:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    public void save(ActiveLiveSearch search) {
        try {
            String json = objectMapper.writeValueAsString(search);
            redisTemplate.opsForValue().set(PREFIX + search.getBookerId(), json, SEARCH_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise ActiveLiveSearch bookerId={}", search.getBookerId(), e);
        }
    }

    public Optional<ActiveLiveSearch> findByBookerId(Long bookerId) {
        String json = redisTemplate.opsForValue().get(PREFIX + bookerId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ActiveLiveSearch.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialise ActiveLiveSearch: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isSearchActive(Long bookerId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + bookerId));
    }

    public void delete(Long bookerId) {
        redisTemplate.delete(PREFIX + bookerId);
    }

    /**
     * Adds rideId to the comma-separated notifiedRideIds string and re-saves.
     * Returns false if the search session has already expired.
     */
    public boolean markRideNotified(Long bookerId, Long rideId) {
        Optional<ActiveLiveSearch> opt = findByBookerId(bookerId);
        if (opt.isEmpty()) return false;

        ActiveLiveSearch search = opt.get();
        String existing = search.getNotifiedRideIds();
        search.setNotifiedRideIds(
                (existing == null || existing.isBlank())
                        ? String.valueOf(rideId)
                        : existing + "," + rideId
        );

        // Preserve remaining TTL instead of resetting to 3 min
        long remainingMillis = search.getExpiresAt() - System.currentTimeMillis();
        if (remainingMillis <= 0) return false;

        try {
            String json = objectMapper.writeValueAsString(search);
            redisTemplate.opsForValue().set(
                    PREFIX + bookerId, json, Duration.ofMillis(remainingMillis));
        } catch (JsonProcessingException e) {
            log.error("Failed to update notifiedRideIds: {}", e.getMessage());
        }
        return true;
    }

    public Set<Long> getNotifiedRideIds(Long bookerId) {
        return findByBookerId(bookerId)
                .map(s -> {
                    if (s.getNotifiedRideIds() == null || s.getNotifiedRideIds().isBlank())
                        return Collections.<Long>emptySet();
                    Set<Long> ids = new HashSet<>();
                    for (String part : s.getNotifiedRideIds().split(",")) {
                        try { ids.add(Long.parseLong(part.trim())); }
                        catch (NumberFormatException ignored) {}
                    }
                    return ids;
                })
                .orElse(Collections.emptySet());
    }
}