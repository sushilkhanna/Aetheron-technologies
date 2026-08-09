package com.bikepooling.scheduler;

import com.bikepooling.dto.request.ActiveLiveSearch;
import com.bikepooling.repository.ActiveLiveSearchRepository;
import com.bikepooling.service.LiveSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Runs every 10 seconds. For each booker with an active search session,
 * fires the match pipeline against all current live rides.
 *
 * 10-second interval is a balance:
 *   - Short enough that a new live driver is found within ~10 sec
 *   - Long enough to avoid hammering OSRM on every tick
 *
 * Redis TTL handles expiry — when a search session's 3-min TTL elapses,
 * the key disappears and SCAN stops finding it naturally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveSearchScheduler {

    private final StringRedisTemplate        redisTemplate;
    private final ActiveLiveSearchRepository searchRepo;
    private final LiveSearchService          searchService;

    @Scheduled(fixedDelay = 10_000)   // 10 seconds after previous run completes
    public void tick() {
        // Scan all active search keys
        Set<String> keys = redisTemplate.keys("live:search:*");
        if (keys == null || keys.isEmpty()) return;

        log.debug("LiveSearchScheduler tick: {} active search session(s)", keys.size());

        for (String key : keys) {
            // key = "live:search:{bookerId}"
            String bookerIdStr = key.substring("live:search:".length());
            try {
                Long bookerId = Long.parseLong(bookerIdStr);
                searchRepo.findByBookerId(bookerId).ifPresent(session ->
                        runCycleForBooker(bookerId, session));
            } catch (NumberFormatException e) {
                log.warn("Unexpected key format in Redis: {}", key);
            }
        }
    }

    private void runCycleForBooker(Long bookerId, ActiveLiveSearch session) {
        try {
            searchService.runMatchCycleForBooker(bookerId, session);
        } catch (Exception e) {
            // Never let one booker's failure crash the whole scheduler tick
            log.error("Match cycle failed for bookerId={}: {}", bookerId, e.getMessage());
        }
    }
}