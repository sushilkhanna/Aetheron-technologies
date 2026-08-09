package com.bikepooling.service;

import com.bikepooling.dto.request.AdminMetricsDTO;
import com.bikepooling.enums.RideState;
import com.bikepooling.repository.RideRepository;
import com.bikepooling.repository.RideStatusRepository;
import com.bikepooling.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMetricsService {

    private final StringRedisTemplate  redis;
    private final UserRepository       userRepo;
    private final RideRepository       rideRepo;
    private final RideStatusRepository rideStatusRepo;

    private static final String KEY_TOTAL_USERS   = "admin:metric:total_users";
    private static final String KEY_ACTIVE_RIDES  = "admin:metric:active_rides";
    private static final String KEY_RIDES_TODAY   = "admin:metric:rides_today";
    private static final String KEY_REVENUE_TODAY = "admin:metric:revenue_today";

    private static final List<RideState> ACTIVE_STATES = List.of(
            RideState.OPEN,
            RideState.BOOKED,
            RideState.STARTED
    );


    @PostConstruct
    public void seedOnStartup() {
        log.info("Seeding admin metrics into Redis...");

        seedIfAbsent(KEY_TOTAL_USERS, () ->
                userRepo.countActiveUsers());

        seedIfAbsent(KEY_ACTIVE_RIDES, () ->
                rideStatusRepo.countByStateIn(ACTIVE_STATES));

        seedIfAbsent(KEY_RIDES_TODAY, () ->
                rideRepo.countRidesToday());

        seedIfAbsent(KEY_REVENUE_TODAY, () ->
                Optional.ofNullable(rideRepo.sumRevenueToday(RideState.COMPLETED))
                        .orElse(BigDecimal.ZERO).longValue());

        log.info("Admin metrics seeding complete");
    }

    private void seedIfAbsent(String key, java.util.function.LongSupplier dbQuery) {
        if (!Boolean.TRUE.equals(redis.hasKey(key))) {
            long val = dbQuery.getAsLong();
            redis.opsForValue().set(key, String.valueOf(val));
            log.info("Seeded {} = {}", key, val);
        } else {
            log.info("{} already in Redis, skipping seed", key);
        }
    }


    @Scheduled(cron = "0 0 0 * * *")   // fires at 00:00:00 every day
    public void resetDailyMetrics() {
        redis.opsForValue().set(KEY_RIDES_TODAY,   "0");
        redis.opsForValue().set(KEY_REVENUE_TODAY, "0");
        log.info("Daily metrics reset at midnight");
    }


    public long getTotalUsers() {
        return getLong(KEY_TOTAL_USERS, () -> userRepo.countActiveUsers());
    }

    public long getActiveRides() {
        return getLong(KEY_ACTIVE_RIDES, () -> rideStatusRepo.countByStateIn(ACTIVE_STATES));
    }

    public long getRidesToday() {
        return getLong(KEY_RIDES_TODAY, () -> rideRepo.countRidesToday());
    }

    public BigDecimal getRevenueToday() {
        String cached = redis.opsForValue().get(KEY_REVENUE_TODAY);
        if (cached != null) return new BigDecimal(cached);

        log.warn("revenue_today evicted, rebuilding from DB");
        BigDecimal revenue = Optional.ofNullable(rideRepo.sumRevenueToday(RideState.COMPLETED))
                .orElse(BigDecimal.ZERO);
        redis.opsForValue().set(KEY_REVENUE_TODAY, revenue.toPlainString());
        return revenue;
    }

    public AdminMetricsDTO getAll() {
        return AdminMetricsDTO.builder()
                .totalUsers(getTotalUsers())
                .activeRides(getActiveRides())
                .ridesToday(getRidesToday())
                .revenueToday(getRevenueToday())
                .build();
    }

    // ── event hooks — called from RideService / AuthService ──────────────────

    // called after user saved to DB
    public void onUserRegistered() {
        safeIncrement(KEY_TOTAL_USERS);
    }

    // called after user deactivated
    public void onUserDeactivated() {
        safeDecrement(KEY_TOTAL_USERS);
    }


    public void onRidePosted(LocalDateTime departAt) {
         safeIncrement(KEY_ACTIVE_RIDES);

        if (departAt.toLocalDate().equals(LocalDate.now())) {
            safeIncrement(KEY_RIDES_TODAY);
        }

        log.debug("Ride posted — active_rides++, rides_today {}",
                departAt.toLocalDate().equals(LocalDate.now()) ? "++" : "unchanged (future ride)");
    }


    public void onRideCompleted(BigDecimal fare) {
        safeDecrement(KEY_ACTIVE_RIDES);

        if (fare != null && fare.compareTo(BigDecimal.ZERO) > 0) {
            try {
                String current = redis.opsForValue().get(KEY_REVENUE_TODAY);
                BigDecimal currentRevenue = current != null
                        ? new BigDecimal(current)
                        : BigDecimal.ZERO;
                BigDecimal updated = currentRevenue.add(fare);
                redis.opsForValue().set(KEY_REVENUE_TODAY, updated.toPlainString());
                log.debug("Ride completed — active_rides--, revenue_today += {}", fare);
            } catch (Exception e) {
                log.error("Failed to update revenue_today in Redis", e);
            }
        }
    }


    public void onRideCancelled() {
        safeDecrement(KEY_ACTIVE_RIDES);
        log.debug("Ride cancelled — active_rides--");
    }

    private long getLong(String key, java.util.function.LongSupplier fallback) {
        String cached = redis.opsForValue().get(key);
        if (cached != null) return Long.parseLong(cached);

        log.warn("{} evicted from Redis, rebuilding from DB", key);
        long val = fallback.getAsLong();
        redis.opsForValue().set(key, String.valueOf(val));
        return val;
    }

    private void safeIncrement(String key) {
        try {
            redis.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Failed to increment {} in Redis", key, e);
        }
    }

    private void safeDecrement(String key) {
        try {
            redis.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("Failed to decrement {} in Redis", key, e);
        }
    }
}