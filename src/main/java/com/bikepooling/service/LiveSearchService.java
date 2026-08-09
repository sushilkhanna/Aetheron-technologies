package com.bikepooling.service;

import com.bikepooling.config.OsrmClient;
import com.bikepooling.dto.request.ActiveLiveSearch;
import com.bikepooling.dto.request.LiveSearchRequest;
import com.bikepooling.dto.response.LiveRideSnapshot;
import com.bikepooling.entity.User;
import com.bikepooling.enums.Gender;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.ActiveLiveSearchRepository;
import com.bikepooling.repository.LiveRideRedisRepository;
import com.bikepooling.repository.UserRepository;
import com.bikepooling.util.RouteMatchUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class LiveSearchService {

    private static final int MAX_ETA_MINUTES = 15;

    private final ActiveLiveSearchRepository searchRepo;
    private final LiveRideRedisRepository    liveRideRepo;
    private final UserRepository             userRepo;
    private final FcmService                 fcmService;
    private final OsrmClient                 osrmClient;
    private final Executor                   osrmExecutor;

    public LiveSearchService(
            ActiveLiveSearchRepository searchRepo,
            LiveRideRedisRepository liveRideRepo,
            UserRepository userRepo,
            FcmService fcmService,
            OsrmClient osrmClient,
            @Qualifier("osrmExecutor") Executor osrmExecutor) {
        this.searchRepo   = searchRepo;
        this.liveRideRepo = liveRideRepo;
        this.userRepo     = userRepo;
        this.fcmService   = fcmService;
        this.osrmClient   = osrmClient;
        this.osrmExecutor = osrmExecutor;
    }

    public void startSearch(LiveSearchRequest req, Long bookerId) {

        if (searchRepo.isSearchActive(bookerId)) {
            throw AppException.conflict(
                    "You already have an active search session. " +
                            "Wait for it to expire or cancel it first.");
        }

        long now     = System.currentTimeMillis();
        long expires = now + ActiveLiveSearchRepository.SEARCH_TTL.toMillis();

        ActiveLiveSearch session = ActiveLiveSearch.builder()
                .bookerId(bookerId)
                .pickupLat(req.getPickupLat().doubleValue())
                .pickupLng(req.getPickupLng().doubleValue())
                .dropLat(req.getDropLat().doubleValue())
                .dropLng(req.getDropLng().doubleValue())
                .pickupName(req.getPickupName())
                .dropName(req.getDropName())
                .notifiedRideIds("")
                .createdAt(now)
                .expiresAt(expires)
                .build();

        searchRepo.save(session);
        log.info("Live search started: bookerId={} pickup=({},{}) drop=({},{})",
                bookerId, req.getPickupLat(), req.getPickupLng(),
                req.getDropLat(), req.getDropLng());

        runMatchCycleForBooker(bookerId, session);
    }

    public void cancelSearch(Long bookerId) {
        searchRepo.delete(bookerId);
        log.info("Live search cancelled by booker: bookerId={}", bookerId);
    }

    public void runMatchCycleForAllActiveSearches() {
        List<LiveRideSnapshot> allLiveRides = liveRideRepo.findAllLive();
        if (allLiveRides.isEmpty()) return;
        // Scheduler drives per-booker cycles via runMatchCycleForBooker.
    }

    public void runMatchCycleForBooker(Long bookerId, ActiveLiveSearch session) {

        List<LiveRideSnapshot> allLive = liveRideRepo.findAllLive();
        if (allLive.isEmpty()) return;

        Set<Long> alreadyNotified = searchRepo.getNotifiedRideIds(bookerId);

        User booker = userRepo.findById(bookerId).orElse(null);
        if (booker == null) return;

        List<LiveRideSnapshot> stage1 = allLive.stream()
                .filter(ride -> !alreadyNotified.contains(ride.getRideId()))
                .filter(ride -> passesGenderFilter(ride, booker))
                .filter(ride -> RouteMatchUtil.evaluateStage1(
                        ride.getMeta().getFromLat(), ride.getMeta().getFromLng(),
                        ride.getMeta().getToLat(),   ride.getMeta().getToLng(),
                        ride.getMeta().getDistanceKm(),
                        session.getPickupLat(), session.getPickupLng(),
                        session.getDropLat(),   session.getDropLng()
                ).isMatched())
                .toList();

        if (stage1.isEmpty()) return;

        List<CompletableFuture<Void>> futures = stage1.stream()
                .map(ride -> CompletableFuture.runAsync(() ->
                        checkDetourAndEta(ride, session, bookerId), osrmExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void checkDetourAndEta(LiveRideSnapshot ride,
                                   ActiveLiveSearch session,
                                   Long bookerId) {
        try {
            OsrmClient.RouteLegs legs = osrmClient.getRouteLegs(
                    new double[]{ride.getMeta().getFromLat(), session.getPickupLat(),
                            session.getDropLat(), ride.getMeta().getToLat()},
                    new double[]{ride.getMeta().getFromLng(), session.getPickupLng(),
                            session.getDropLng(), ride.getMeta().getToLng()}
            );

            double detourKm = Math.max(legs.getTotalKm() - ride.getMeta().getDistanceKm(), 0.0);
            RouteMatchUtil.MatchResult detourResult =
                    RouteMatchUtil.checkDetourBudget(detourKm, ride.getMeta().getExtraDistanceKm());

            if (!detourResult.isMatched()) {
                log.debug("Detour too large for rideId={} bookerId={}: {}km > {}km budget",
                        ride.getRideId(), bookerId, detourKm, ride.getMeta().getExtraDistanceKm());
                return;
            }

            int etaMinutes = osrmClient.getRoadDurationMinutes(
                    ride.getCurrentLat(), ride.getCurrentLng(),
                    session.getPickupLat(), session.getPickupLng()
            );

            if (etaMinutes > MAX_ETA_MINUTES) {
                log.debug("Driver too far for rideId={} bookerId={}: ETA {}min > {}min",
                        ride.getRideId(), bookerId, etaMinutes, MAX_ETA_MINUTES);
                return;
            }

            notifyDriver(ride, session, bookerId, etaMinutes);

        } catch (Exception e) {
            log.error("Error in match pipeline for rideId={} bookerId={}: {}",
                    ride.getRideId(), bookerId, e.getMessage());
        }
    }

    private void notifyDriver(LiveRideSnapshot ride,
                              ActiveLiveSearch session,
                              Long bookerId,
                              int etaMinutes) {

        boolean marked = searchRepo.markRideNotified(bookerId, ride.getRideId());
        if (!marked) return;

        log.info("Notifying driver: rideId={} driverId={} bookerId={} eta={}min",
                ride.getRideId(), ride.getDriverId(), bookerId, etaMinutes);

        fcmService.notifyDriverLiveBookerFound(
                ride.getDriverId(),
                bookerId,
                ride.getRideId(),
                session.getPickupName(),
                session.getDropName(),
                etaMinutes
        );
    }

    private boolean passesGenderFilter(LiveRideSnapshot ride, User booker) {
        String pref = ride.getMeta().getPreferredGender();
        if (pref == null || "ANY".equalsIgnoreCase(pref)) return true;
        if ("FEMALE".equalsIgnoreCase(pref)) {
            return booker.getGender() == Gender.FEMALE;
        }
        return true;
    }
}