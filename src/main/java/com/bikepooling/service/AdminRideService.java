package com.bikepooling.service;

import com.bikepooling.dto.request.AdminRideDTO;
import com.bikepooling.dto.request.AdminRideLocationDTO;
import com.bikepooling.dto.request.AdminRideStatsDTO;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.entity.LiveRide;
import com.bikepooling.entity.ScheduledRideInstance;
import com.bikepooling.enums.LiveRideState;
import com.bikepooling.enums.RideState;
import com.bikepooling.repository.LiveRideRepository;
import com.bikepooling.repository.ScheduledRideInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRideService {

    private final ScheduledRideInstanceRepository instanceRepo;
    private final LiveRideRepository liveRideRepo;
    private final RedisLocationCacheService redisLocationCacheService;
    private final LiveRideCacheService liveRideCacheService;
    private final ScheduledRideLocationCacheService scheduledLocationCacheService;

    public PagedResponse<AdminRideDTO> getRides(
            int page, int size,
            String stateStr, Long driverId,
            String fromStr, String toStr,
            String search, String sortBy, String sortDir) {

        List<AdminRideDTO> allRides = new ArrayList<>();

        // 1. Fetch Scheduled Ride Instances
        List<ScheduledRideInstance> scheduledList = instanceRepo.findAll();
        scheduledList.forEach(inst -> allRides.add(AdminRideDTO.from(inst)));

        // 2. Fetch Live Rides
        List<LiveRide> liveList = liveRideRepo.findAll();
        liveList.forEach(live -> allRides.add(AdminRideDTO.from(live)));

        // 3. Apply Filtering
        Stream<AdminRideDTO> stream = allRides.stream();

        // State filter
        if (stateStr != null && !stateStr.isBlank() && !"all".equalsIgnoreCase(stateStr.trim())) {
            String filterState = stateStr.trim().toUpperCase();
            stream = stream.filter(r -> r.getState() != null && r.getState().equalsIgnoreCase(filterState));
        }

        // Driver ID filter
        if (driverId != null) {
            stream = stream.filter(r -> driverId.equals(r.getDriverId()));
        }

        // Search text filter
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            stream = stream.filter(r ->
                (r.getDriverName() != null && r.getDriverName().toLowerCase().contains(q)) ||
                (r.getBookerName() != null && r.getBookerName().toLowerCase().contains(q)) ||
                (r.getFromName() != null && r.getFromName().toLowerCase().contains(q)) ||
                (r.getToName() != null && r.getToName().toLowerCase().contains(q)) ||
                (r.getInstanceId() != null && String.valueOf(r.getInstanceId()).contains(q))
            );
        }

        // Date range filters
        LocalDateTime fromDate = parseDateTime(fromStr, false);
        LocalDateTime toDate = parseDateTime(toStr, true);

        if (fromDate != null) {
            stream = stream.filter(r -> {
                LocalDateTime t = getRideTime(r);
                return t != null && !t.isBefore(fromDate);
            });
        }

        if (toDate != null) {
            stream = stream.filter(r -> {
                LocalDateTime t = getRideTime(r);
                return t != null && !t.isAfter(toDate);
            });
        }

        List<AdminRideDTO> filteredRides = stream.collect(Collectors.toList());

        // 4. Sorting
        boolean isAsc = "asc".equalsIgnoreCase(sortDir);
        Comparator<AdminRideDTO> comparator;

        if ("departAt".equalsIgnoreCase(sortBy) || "date".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(this::getRideTime, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("driverName".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(r -> r.getDriverName() != null ? r.getDriverName().toLowerCase() : "", Comparator.naturalOrder());
        } else if ("state".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(this::getPriorityRank);
        } else if ("fare".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(r -> r.getFare() != null ? r.getFare() : BigDecimal.ZERO);
        } else if ("distanceKm".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(r -> r.getDistanceKm() != null ? r.getDistanceKm() : BigDecimal.ZERO);
        } else {
            // Default Priority Sorting
            comparator = Comparator.comparingInt(this::getPriorityRank)
                    .thenComparing(this::getRideTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        if (isAsc && sortBy != null && !"default".equalsIgnoreCase(sortBy)) {
            // Comparator is naturally ASC
        } else if (sortBy != null && !"default".equalsIgnoreCase(sortBy)) {
            comparator = comparator.reversed();
        }

        filteredRides.sort(comparator);

        // 5. Pagination
        int pageSize = Math.min(size > 0 ? size : 20, 100);
        int pageNo = Math.max(page, 0);
        int start = Math.min(pageNo * pageSize, filteredRides.size());
        int end = Math.min(start + pageSize, filteredRides.size());

        List<AdminRideDTO> pageContent = filteredRides.subList(start, end);
        Pageable pageable = PageRequest.of(pageNo, pageSize);

        return PagedResponse.of(new PageImpl<>(pageContent, pageable, filteredRides.size()));
    }

    private LocalDateTime parseDateTime(String input, boolean isEndOfDay) {
        if (input == null || input.isBlank()) return null;
        try {
            if (input.contains("T")) {
                return LocalDateTime.parse(input);
            } else {
                LocalDate d = LocalDate.parse(input.trim());
                return isEndOfDay ? d.atTime(LocalTime.MAX) : d.atStartOfDay();
            }
        } catch (Exception e) {
            log.warn("Failed to parse date parameter: {}", input);
            return null;
        }
    }

    private int getPriorityRank(AdminRideDTO ride) {
        String state = ride.getState() != null ? ride.getState().toUpperCase() : "";
        switch (state) {
            case "STARTED":
            case "VERIFIED":
            case "CONFIRMED":
            case "LIVE":
            case "SOS_TRIGGERED":
                return 1; // Priority 1: Active & Moving
            case "OPEN":
                return 2; // Priority 2: Posted
            case "COMPLETED":
                return 3; // Priority 3: Completed
            case "EXPIRED":
            case "CANCELLED":
            default:
                return 4; // Priority 4: Expired & Cancelled
        }
    }

    private LocalDateTime getRideTime(AdminRideDTO ride) {
        if (ride.getStartedAt() != null) return ride.getStartedAt();
        if (ride.getRideDate() != null && ride.getDepartTime() != null) {
            return LocalDateTime.of(ride.getRideDate(), ride.getDepartTime());
        }
        return LocalDateTime.now();
    }

    public AdminRideStatsDTO getStats() {
        long totalLive = liveRideRepo.count();
        long totalScheduled = instanceRepo.count();

        long activeLive = liveRideRepo.findAll().stream()
                .filter(r -> r.getState() == LiveRideState.LIVE || r.getState() == LiveRideState.CONFIRMED || r.getState() == LiveRideState.VERIFIED)
                .count();

        long activeScheduled = instanceRepo.findAll().stream()
                .filter(i -> i.getState() == RideState.OPEN || i.getState() == RideState.BOOKED || i.getState() == RideState.STARTED || i.getState() == RideState.VERIFIED)
                .count();

        return AdminRideStatsDTO.builder()
                .totalToday((int) (totalLive + totalScheduled))
                .completedToday(0)
                .cancelledToday(0)
                .expiredToday(0)
                .activeToday((int) (activeScheduled))
                .liveToday((int) (activeLive))
                .revenueToday(BigDecimal.ZERO)
                .successRatePct(100)
                .daily(List.of())
                .build();
    }

    public AdminRideLocationDTO getRideLocation(Long id) {
        // 1. Check Redis for Live Ride location
        var liveRedis = redisLocationCacheService.getLiveRideLocation(id);
        if (liveRedis != null) {
            return buildLiveLocationDTO(id, liveRedis.getLat(), liveRedis.getLng(), liveRedis.getTimestamp());
        }

        // 2. Check Redis for Scheduled Ride location
        var schedRedis = redisLocationCacheService.getScheduledRideLocation(id);
        if (schedRedis != null) {
            return buildScheduledLocationDTO(id, schedRedis.getLat(), schedRedis.getLng(), schedRedis.getTimestamp());
        }

        // 3. Fallback to in-memory Live Driver session
        Optional<LiveRide> liveOpt = liveRideRepo.findByIdWithDetails(id);
        if (liveOpt.isPresent()) {
            LiveRide live = liveOpt.get();
            double curLat = live.getFromLat().doubleValue();
            double curLng = live.getFromLng().doubleValue();
            var driverSession = liveRideCacheService.getLiveDriver(live.getDriver().getId());
            if (driverSession != null) {
                curLat = driverSession.getCurrentLat();
                curLng = driverSession.getCurrentLng();
            }
            return buildLiveLocationDTO(id, curLat, curLng, System.currentTimeMillis());
        }

        // 4. Fallback to in-memory Scheduled Ride snapshot / DB
        var schedMem = scheduledLocationCacheService.getLocation(id);
        if (schedMem != null) {
            return buildScheduledLocationDTO(id, schedMem.getLat(), schedMem.getLng(), schedMem.getTimestamp());
        }

        Optional<ScheduledRideInstance> instOpt = instanceRepo.findByIdWithDetails(id);
        if (instOpt.isPresent()) {
            ScheduledRideInstance inst = instOpt.get();
            double curLat = inst.getTemplate().getFromLat().doubleValue();
            double curLng = inst.getTemplate().getFromLng().doubleValue();
            return buildScheduledLocationDTO(id, curLat, curLng, System.currentTimeMillis());
        }

        return null;
    }

    public List<AdminRideLocationDTO> getAllActiveLocations() {
        List<AdminRideLocationDTO> activeLocs = new ArrayList<>();

        // Add active live rides
        List<LiveRide> liveList = liveRideRepo.findAll();
        for (LiveRide live : liveList) {
            if (live.getState() == LiveRideState.LIVE || live.getState() == LiveRideState.CONFIRMED || live.getState() == LiveRideState.VERIFIED) {
                AdminRideLocationDTO loc = getRideLocation(live.getId());
                if (loc != null) activeLocs.add(loc);
            }
        }

        // Add active scheduled ride instances
        List<ScheduledRideInstance> scheduledList = instanceRepo.findAll();
        for (ScheduledRideInstance inst : scheduledList) {
            if (inst.getState() == RideState.STARTED || inst.getState() == RideState.VERIFIED || inst.getState() == RideState.BOOKED) {
                AdminRideLocationDTO loc = getRideLocation(inst.getId());
                if (loc != null) activeLocs.add(loc);
            }
        }

        return activeLocs;
    }

    private AdminRideLocationDTO buildScheduledLocationDTO(Long id, double curLat, double curLng, long timestamp) {
        double fromLat = 0, fromLng = 0, toLat = 0, toLng = 0;
        Optional<ScheduledRideInstance> instOpt = instanceRepo.findByIdWithDetails(id);
        if (instOpt.isPresent()) {
            var t = instOpt.get().getTemplate();
            fromLat = t.getFromLat().doubleValue();
            fromLng = t.getFromLng().doubleValue();
            toLat = t.getToLat().doubleValue();
            toLng = t.getToLng().doubleValue();
        }
        return AdminRideLocationDTO.builder()
                .id(id)
                .rideId(id)
                .lat(curLat)
                .lng(curLng)
                .currentLat(curLat)
                .currentLng(curLng)
                .lastUpdatedAt(timestamp)
                .fromLat(fromLat)
                .fromLng(fromLng)
                .toLat(toLat)
                .toLng(toLng)
                .build();
    }

    private AdminRideLocationDTO buildLiveLocationDTO(Long id, double curLat, double curLng, long timestamp) {
        double fromLat = 0, fromLng = 0, toLat = 0, toLng = 0;
        double dropLat = 0, dropLng = 0;
        boolean dropSet = false;

        Optional<LiveRide> liveOpt = liveRideRepo.findByIdWithDetails(id);
        if (liveOpt.isPresent()) {
            var live = liveOpt.get();
            fromLat = live.getFromLat().doubleValue();
            fromLng = live.getFromLng().doubleValue();
            toLat = live.getToLat().doubleValue();
            toLng = live.getToLng().doubleValue();
            if (live.getDropLat() != null && live.getDropLng() != null) {
                dropLat = live.getDropLat().doubleValue();
                dropLng = live.getDropLng().doubleValue();
                dropSet = true;
            }
        }
        return AdminRideLocationDTO.builder()
                .id(id)
                .rideId(id)
                .lat(curLat)
                .lng(curLng)
                .currentLat(curLat)
                .currentLng(curLng)
                .bookerDropLat(dropLat)
                .bookerDropLng(dropLng)
                .bookerDropSet(dropSet)
                .lastUpdatedAt(timestamp)
                .fromLat(fromLat)
                .fromLng(fromLng)
                .toLat(toLat)
                .toLng(toLng)
                .build();
    }
}