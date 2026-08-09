package com.bikepooling.service;

import com.bikepooling.dto.request.AdminRideDTO;
import com.bikepooling.dto.request.AdminRideLocationDTO;
import com.bikepooling.dto.request.AdminRideStatsDTO;
import com.bikepooling.dto.response.LiveRideSnapshot;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.entity.RideStatus;
import com.bikepooling.enums.RideState;
import com.bikepooling.repository.LiveRideRedisRepository;
import com.bikepooling.repository.RideStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRideService {

    private final RideStatusRepository    rideStatusRepo;
    private final LiveRideRedisRepository liveRideRepo;

    private static final int GRAPH_DAYS = 7;

    // ── Ride table (now with free-text search) ──────────────────────────────

    public PagedResponse<AdminRideDTO> getRides(
            int page, int size,
            String stateStr, Long driverId,
            LocalDateTime from, LocalDateTime to,
            String search) {

        RideState state = stateStr != null ? RideState.valueOf(stateStr.toUpperCase()) : null;
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(size > 0 ? size : 20, 50),
                Sort.by("ride.departAt").descending()
        );

        String keyword = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        Page<RideStatus> dbPage =
                rideStatusRepo.searchForAdmin(state, driverId, from, to, keyword, pageable);

        return PagedResponse.of(dbPage.map(AdminRideDTO::from));
    }

    // ── KPI cards + graph ─────────────────────────────────────────────────────

    public AdminRideStatsDTO getStats() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd   = dayStart.plusDays(1);

        List<Object[]> todayRows =
                rideStatusRepo.countByStateForDay(dayStart, dayEnd);

        Map<RideState, Long> todayCounts = new EnumMap<>(RideState.class);
        for (Object[] row : todayRows) {
            todayCounts.put((RideState) row[0], (Long) row[1]);
        }

        long completed = get(todayCounts, RideState.COMPLETED);
        long cancelled = get(todayCounts, RideState.CANCELLED);
        long expired   = get(todayCounts, RideState.EXPIRED);
        long open      = get(todayCounts, RideState.OPEN);
        long live      = get(todayCounts, RideState.LIVE);
        long booked    = get(todayCounts, RideState.BOOKED);
        long started   = get(todayCounts, RideState.STARTED);
        long verified  = get(todayCounts, RideState.VERIFIED);
        long active    = open + live + booked + started + verified;
        long total     = completed + cancelled + expired + active;

        long closedRides = completed + cancelled + expired;
        int successRate  = closedRides > 0
                ? (int) Math.round(completed * 100.0 / closedRides) : 0;

        BigDecimal revenueToday = rideStatusRepo.sumRevenueForPeriod(dayStart, dayEnd);

        LocalDateTime graphFrom = dayStart.minusDays(GRAPH_DAYS - 1);
        List<Object[]> graphRows = rideStatusRepo.dailyStatsByState(graphFrom);

        Map<LocalDate, BigDecimal> revenueByDay = buildRevenueByDay(graphFrom, dayEnd);

        List<AdminRideStatsDTO.DailyRideStat> daily =
                buildDailyStats(graphRows, revenueByDay, graphFrom);

        return AdminRideStatsDTO.builder()
                .totalToday(total)
                .completedToday(completed)
                .cancelledToday(cancelled)
                .expiredToday(expired)
                .activeToday(active)
                .liveToday(live)
                .revenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
                .successRatePct(successRate)
                .daily(daily)
                .build();
    }

    // ── Location from Redis (for map view) ────────────────────────────────────

    public AdminRideLocationDTO getRideLocation(Long rideId) {
        LiveRideSnapshot snap = liveRideRepo.findSnapshot(rideId);
        if (snap == null) return null;

        return AdminRideLocationDTO.builder()
                .rideId(rideId)
                .currentLat(snap.getCurrentLat())
                .currentLng(snap.getCurrentLng())
                .bookerDropLat(snap.getMeta().getBookerDropLat())
                .bookerDropLng(snap.getMeta().getBookerDropLng())
                .bookerDropSet(snap.getMeta().isBookerDropSet())
                .lastUpdatedAt(snap.getLastUpdatedAt())
                .fromLat(snap.getMeta().getFromLat())
                .fromLng(snap.getMeta().getFromLng())
                .toLat(snap.getMeta().getToLat())
                .toLng(snap.getMeta().getToLng())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long get(Map<RideState, Long> map, RideState key) {
        return map.getOrDefault(key, 0L);
    }

    private Map<LocalDate, BigDecimal> buildRevenueByDay(LocalDateTime from, LocalDateTime to) {
        return Map.of();
    }

    private List<AdminRideStatsDTO.DailyRideStat> buildDailyStats(
            List<Object[]> rows,
            Map<LocalDate, BigDecimal> revenueByDay,
            LocalDateTime graphFrom) {

        Map<LocalDate, long[]> byDate = new LinkedHashMap<>();
        for (int i = 0; i < GRAPH_DAYS; i++) {
            byDate.put(graphFrom.plusDays(i).toLocalDate(), new long[3]);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Object[] row : rows) {
            LocalDate date  = LocalDate.parse(row[0].toString(), fmt);
            RideState state = RideState.valueOf(row[1].toString());
            long count      = ((Number) row[2]).longValue();

            long[] arr = byDate.get(date);
            if (arr == null) continue;
            if (state == RideState.COMPLETED) arr[0] += count;
            if (state == RideState.CANCELLED) arr[1] += count;
            if (state == RideState.EXPIRED)   arr[2] += count;
        }

        DateTimeFormatter label = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
        return byDate.entrySet().stream()
                .map(e -> AdminRideStatsDTO.DailyRideStat.builder()
                        .date(e.getKey().format(label))
                        .completed(e.getValue()[0])
                        .cancelled(e.getValue()[1])
                        .expired(e.getValue()[2])
                        .earning(revenueByDay.getOrDefault(e.getKey(), BigDecimal.ZERO))
                        .build())
                .toList();
    }
}