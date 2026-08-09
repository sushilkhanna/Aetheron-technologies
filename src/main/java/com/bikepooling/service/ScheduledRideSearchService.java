package com.bikepooling.service;

import com.bikepooling.config.OsrmClient;
import com.bikepooling.dto.request.SearchScheduledRideRequest;
import com.bikepooling.dto.response.ScheduledRideSearchResultItem;
import com.bikepooling.dto.response.ScheduledRideSearchResponse;
import com.bikepooling.entity.ScheduledRideInstance;
import com.bikepooling.entity.ScheduledRideTemplate;
import com.bikepooling.entity.User;
import com.bikepooling.enums.Gender;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.ScheduledRideInstanceRepository;
import com.bikepooling.repository.UserRepository;
import com.bikepooling.util.RouteMatchUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledRideSearchService {

    private final ScheduledRideInstanceRepository instanceRepo;
    private final UserRepository userRepo;
    private final OsrmClient osrmClient;

    @Qualifier("osrmExecutor")
    private final Executor osrmExecutor;

    public ScheduledRideSearchResponse search(SearchScheduledRideRequest req, Long userId) {

        if (!req.getWindowTo().isAfter(req.getWindowFrom())) {
            throw AppException.badRequest("Window end time must be after window start time.");
        }

        boolean hasMultiDates = req.getDates() != null && !req.getDates().isEmpty();
        boolean hasSingleDate = req.getDate() != null;
        boolean hasWantedDays = req.getWantedDays() != null && !req.getWantedDays().isEmpty();

        if (!hasMultiDates && !hasSingleDate && !hasWantedDays) {
            throw AppException.badRequest("Provide dates, a specific date, or at least one wantedDay.");
        }
        if (hasSingleDate && req.getDate().isBefore(LocalDate.now())) {
            throw AppException.badRequest("Date cannot be in the past.");
        }
        if (hasMultiDates) {
            LocalDate today = LocalDate.now();
            for (LocalDate d : req.getDates()) {
                if (d.isBefore(today)) {
                    throw AppException.badRequest("Date " + d + " cannot be in the past.");
                }
            }
        }

        User booker = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (hasMultiDates) {
            return searchMultiDate(req, userId, booker);
        }

        return searchLegacy(req, userId, booker);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Multi-date search: exact matches + suggestions grouped by Driver/Template
    // ═══════════════════════════════════════════════════════════════════════════

    private ScheduledRideSearchResponse searchMultiDate(
            SearchScheduledRideRequest req, Long userId, User booker) {

        Set<LocalDate> requestedDates = req.getDates();

        List<ScheduledRideInstance> allCandidates = instanceRepo.searchOpenInstancesByDates(
                requestedDates, req.getWindowFrom(), req.getWindowTo(), userId);

        log.info("Multi-date search by userId={}: dates={} window=[{},{}] -> candidatesFound={}",
                userId, requestedDates, req.getWindowFrom(), req.getWindowTo(), allCandidates.size());

        List<ScheduledRideInstance> routeMatched = filterByRoute(allCandidates, req, userId, booker);

        log.info("Multi-date search: {} instances passed route filtering out of {}",
                routeMatched.size(), allCandidates.size());

        // Group route-matched instances by Driver Template
        Map<Long, List<ScheduledRideInstance>> byTemplate = routeMatched.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getTemplate().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ScheduledRideSearchResultItem> exactMatches = byTemplate.entrySet().stream()
                .map(e -> ScheduledRideSearchResultItem.from(e.getValue().get(0).getTemplate(), e.getValue()))
                .toList();

        Set<LocalDate> coveredDates = routeMatched.stream()
                .map(ScheduledRideInstance::getRideDate)
                .collect(Collectors.toSet());

        Set<LocalDate> uncoveredDates = new LinkedHashSet<>(requestedDates);
        uncoveredDates.removeAll(coveredDates);

        List<ScheduledRideSearchResultItem> suggestions = new ArrayList<>();
        if (!uncoveredDates.isEmpty()) {
            suggestions = findSuggestions(uncoveredDates, req, userId, booker);
        }

        Set<LocalDate> stillUncovered = new LinkedHashSet<>(uncoveredDates);
        for (ScheduledRideSearchResultItem s : suggestions) {
            if (s.getAvailableDates() != null) {
                stillUncovered.removeAll(s.getAvailableDates());
            }
        }

        log.info("Multi-date search completed: exactMatchDrivers={} suggestionDrivers={} uncoveredDates={}",
                exactMatches.size(), suggestions.size(), stillUncovered);

        return ScheduledRideSearchResponse.builder()
                .exactMatches(exactMatches)
                .suggestions(suggestions)
                .uncoveredDates(stillUncovered.isEmpty() ? null : stillUncovered)
                .build();
    }

    private List<ScheduledRideSearchResultItem> findSuggestions(
            Set<LocalDate> uncoveredDates, SearchScheduledRideRequest req,
            Long userId, User booker) {

        List<ScheduledRideInstance> suggestionCandidates = instanceRepo.searchOpenInstancesByDates(
                uncoveredDates, req.getWindowFrom(), req.getWindowTo(), userId);

        if (suggestionCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScheduledRideInstance> routeMatched = filterByRoute(
                suggestionCandidates, req, userId, booker);

        Map<Long, List<ScheduledRideInstance>> byTemplate = routeMatched.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getTemplate().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return byTemplate.entrySet().stream()
                .map(e -> ScheduledRideSearchResultItem.from(e.getValue().get(0).getTemplate(), e.getValue()))
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Legacy search (single date / wantedDays) — grouped by Driver/Template
    // ═══════════════════════════════════════════════════════════════════════════

    private ScheduledRideSearchResponse searchLegacy(
            SearchScheduledRideRequest req, Long userId, User booker) {

        List<ScheduledRideInstance> candidates;
        if (req.getDate() != null) {
            candidates = instanceRepo.searchOpenInstancesByDate(
                    req.getDate(), req.getWindowFrom(), req.getWindowTo(), userId);
        } else if (req.getWantedDays() != null && !req.getWantedDays().isEmpty()) {
            candidates = instanceRepo.searchOpenInstancesByDays(
                    req.getWantedDays(), req.getWindowFrom(), req.getWindowTo(), userId);
        } else {
            candidates = instanceRepo.searchOpenInstancesAll(
                    req.getWindowFrom(), req.getWindowTo(), userId);
        }

        log.info("Scheduled ride search by userId={}: date={} wantedDays={} window=[{},{}] -> candidateInstancesFound={}",
                userId, req.getDate(), req.getWantedDays(), req.getWindowFrom(), req.getWindowTo(), candidates.size());

        List<ScheduledRideInstance> routeMatched = filterByRoute(candidates, req, userId, booker);

        Map<Long, List<ScheduledRideInstance>> byTemplate = routeMatched.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getTemplate().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ScheduledRideSearchResultItem> results = byTemplate.entrySet().stream()
                .map(e -> ScheduledRideSearchResultItem.from(e.getValue().get(0).getTemplate(), e.getValue()))
                .toList();

        log.info("Scheduled ride search completed: {} matching drivers/templates returned.", results.size());

        return ScheduledRideSearchResponse.builder()
                .exactMatches(results)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Shared route filtering: Stage 1 (bounding box + gender) → Stage 2 (OSRM detour)
    // ═══════════════════════════════════════════════════════════════════════════

    private List<ScheduledRideInstance> filterByRoute(
            List<ScheduledRideInstance> candidates, SearchScheduledRideRequest req,
            Long userId, User booker) {

        List<ScheduledRideInstance> stage1 = candidates.stream()
                .filter(inst -> {
                    if (!passesGenderFilter(inst.getTemplate(), booker)) {
                        log.debug("Instance #{} failed gender filter for booker #{}", inst.getId(), userId);
                        return false;
                    }
                    RouteMatchUtil.MatchResult res = RouteMatchUtil.evaluateStage1(
                            inst.getTemplate().getFromLat().doubleValue(),
                            inst.getTemplate().getFromLng().doubleValue(),
                            inst.getTemplate().getToLat().doubleValue(),
                            inst.getTemplate().getToLng().doubleValue(),
                            inst.getTemplate().getDistanceKm().doubleValue(),
                            req.getSourceLat().doubleValue(), req.getSourceLng().doubleValue(),
                            req.getDestinationLat().doubleValue(), req.getDestinationLng().doubleValue()
                    );
                    if (!res.isMatched()) {
                        log.debug("Instance #{} failed Stage 1 route match: {}", inst.getId(), res.getReason());
                        return false;
                    }
                    return true;
                })
                .toList();

        log.info("Stage 1 (bounding box & gender) passed: {} / {}", stage1.size(), candidates.size());

        List<CompletableFuture<ScheduledRideInstance>> futures = stage1.stream()
                .map(inst -> CompletableFuture.supplyAsync(() -> {
                    ScheduledRideTemplate t = inst.getTemplate();
                    OsrmClient.RouteLegs legs = osrmClient.getRouteLegs(
                            new double[]{t.getFromLat().doubleValue(),
                                    req.getSourceLat().doubleValue(),
                                    req.getDestinationLat().doubleValue(),
                                    t.getToLat().doubleValue()},
                            new double[]{t.getFromLng().doubleValue(),
                                    req.getSourceLng().doubleValue(),
                                    req.getDestinationLng().doubleValue(),
                                    t.getToLng().doubleValue()});
                    double detourKm = Math.max(legs.getTotalKm() - t.getDistanceKm().doubleValue(), 0.0);
                    RouteMatchUtil.MatchResult res = RouteMatchUtil.checkDetourBudget(
                            detourKm, inst.getExtraDistanceKm().doubleValue());
                    if (!res.isMatched()) {
                        log.debug("Instance #{} failed Stage 2 detour check: {}", inst.getId(), res.getReason());
                        return null;
                    }
                    return inst;
                }, osrmExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean passesGenderFilter(ScheduledRideTemplate template, User booker) {
        String pref = template.getPreferredGender().name();
        if ("ANY".equals(pref)) return true;
        if ("FEMALE".equals(pref)) return booker.getGender() == Gender.FEMALE;
        return true;
    }
}
