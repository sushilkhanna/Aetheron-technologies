package com.bikepooling.service;

import com.bikepooling.dto.request.AdminRideDTO;
import com.bikepooling.dto.request.AdminRideLocationDTO;
import com.bikepooling.dto.request.AdminRideStatsDTO;
import com.bikepooling.dto.request.SendRideNotificationRequest;
import com.bikepooling.dto.response.AdminMessageResponse;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.entity.LiveRide;
import com.bikepooling.entity.ScheduledRideInstance;
import com.bikepooling.entity.User;
import com.bikepooling.enums.LiveRideState;
import com.bikepooling.enums.RideState;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.LiveRideRepository;
import com.bikepooling.repository.ScheduledRideInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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
    private final FcmService fcmService;
    private final Msg91SmsClient msg91SmsClient;

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

        // Use COUNT queries instead of loading all records into memory
        long activeLive = liveRideRepo.countByStateIn(
                List.of(LiveRideState.LIVE, LiveRideState.CONFIRMED, LiveRideState.VERIFIED));

        long activeScheduled = instanceRepo.countByStateIn(
                List.of(RideState.OPEN, RideState.BOOKED, RideState.STARTED, RideState.VERIFIED));

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

        // Use pre-filtered queries instead of findAll() + Java filtering
        List<LiveRide> liveList = liveRideRepo.findAllActiveLiveRides();
        for (LiveRide live : liveList) {
            AdminRideLocationDTO loc = getRideLocation(live.getId());
            if (loc != null) activeLocs.add(loc);
        }

        List<ScheduledRideInstance> scheduledList = instanceRepo.findAllActiveInstances();
        for (ScheduledRideInstance inst : scheduledList) {
            AdminRideLocationDTO loc = getRideLocation(inst.getId());
            if (loc != null) activeLocs.add(loc);
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

    @Transactional(readOnly = true)
    public AdminMessageResponse sendRideNotification(SendRideNotificationRequest request) {
        if (request.getRideSelections() == null || request.getRideSelections().isEmpty()) {
            throw AppException.badRequest("At least one ride must be selected");
        }

        String roleFilter = request.getTargetRole() != null ? request.getTargetRole().trim().toUpperCase() : "BOTH";
        boolean includeDriver = "DRIVER".equals(roleFilter) || "BOTH".equals(roleFilter);
        boolean includeBooker = "BOOKER".equals(roleFilter) || "BOTH".equals(roleFilter);

        // Group IDs by ride type to execute bulk batch queries (avoiding N+1 DB calls)
        List<Long> scheduledIds = request.getRideSelections().stream()
                .filter(s -> s != null && "SCHEDULED".equalsIgnoreCase(s.getRideType()) && s.getInstanceId() != null)
                .map(SendRideNotificationRequest.RideSelection::getInstanceId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> liveIds = request.getRideSelections().stream()
                .filter(s -> s != null && "LIVE".equalsIgnoreCase(s.getRideType()) && s.getInstanceId() != null)
                .map(SendRideNotificationRequest.RideSelection::getInstanceId)
                .distinct()
                .collect(Collectors.toList());

        Set<User> targetUsers = new HashSet<>();

        // 1. Bulk fetch scheduled rides
        if (!scheduledIds.isEmpty()) {
            List<ScheduledRideInstance> instances = instanceRepo.findAllById(scheduledIds);
            for (ScheduledRideInstance inst : instances) {
                if (includeDriver && inst.getTemplate() != null && inst.getTemplate().getPostedBy() != null) {
                    User driver = inst.getTemplate().getPostedBy();
                    if (driver.isActive()) {
                        targetUsers.add(driver);
                    }
                }
                if (includeBooker && inst.getBookedBy() != null) {
                    User booker = inst.getBookedBy();
                    if (booker.isActive()) {
                        targetUsers.add(booker);
                    }
                }
            }
        }

        // 2. Bulk fetch live rides
        if (!liveIds.isEmpty()) {
            List<LiveRide> liveRides = liveRideRepo.findAllById(liveIds);
            for (LiveRide live : liveRides) {
                if (includeDriver && live.getDriver() != null) {
                    User driver = live.getDriver();
                    if (driver.isActive()) {
                        targetUsers.add(driver);
                    }
                }
                if (includeBooker && live.getBooker() != null) {
                    User booker = live.getBooker();
                    if (booker.isActive()) {
                        targetUsers.add(booker);
                    }
                }
            }
        }

        if (targetUsers.isEmpty()) {
            log.warn("[ADMIN RIDE NOTIFICATION] No active matching users (drivers/bookers) found for selected rides");
            return AdminMessageResponse.builder()
                    .totalTargetUsers(0)
                    .sentPushCount(0)
                    .sentSmsCount(0)
                    .failedCount(0)
                    .statusMessage("No active users found matching the role criteria for selected rides")
                    .build();
        }

        int pushCount = 0;
        int smsCount = 0;
        int failedCount = 0;

        String title = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle().trim()
                : "Ride Announcement";

        String message = request.getMessage() != null ? request.getMessage().trim() : "";

        for (User u : targetUsers) {
            boolean success = false;

            // Push Notification via FCM
            if (request.isSendPush()) {
                try {
                    fcmService.sendToUser(
                            u.getId(),
                            title,
                            message,
                            Map.of("type", "ADMIN_ANNOUNCEMENT")
                    );
                    pushCount++;
                    success = true;
                } catch (Exception e) {
                    log.error("[ADMIN RIDE NOTIFICATION - FCM] Failed to send FCM push to userId={}: {}", u.getId(), e.getMessage());
                }
            }

            // SMS via MSG91
            if (request.isSendSms()) {
                if (u.getPhone() == null || u.getPhone().isBlank()) {
                    log.warn("[ADMIN RIDE NOTIFICATION - SMS] Skipped SMS for userId={} — phone number is empty", u.getId());
                } else {
                    try {
                        boolean smsOk = msg91SmsClient.sendSms(u.getPhone(), message, "ADMIN RIDE NOTIFICATION");
                        if (smsOk) {
                            smsCount++;
                            success = true;
                        }
                    } catch (Exception e) {
                        log.error("[ADMIN RIDE NOTIFICATION - SMS] Failed to send MSG91 SMS to userId={}: {}", u.getId(), e.getMessage());
                    }
                }
            }

            if (!success) {
                failedCount++;
            }
        }

        return AdminMessageResponse.builder()
                .totalTargetUsers(targetUsers.size())
                .sentPushCount(pushCount)
                .sentSmsCount(smsCount)
                .failedCount(failedCount)
                .statusMessage(String.format("Ride notification processed for %d participants (Push: %d, SMS: %d)",
                        targetUsers.size(), pushCount, smsCount))
                .build();
    }
}