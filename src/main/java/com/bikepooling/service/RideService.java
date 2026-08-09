package com.bikepooling.service;

import com.bikepooling.config.OsrmClient;
import com.bikepooling.dto.request.PostRideRequest;
import com.bikepooling.dto.request.RideSearchRequest;
import com.bikepooling.dto.request.UpdateRideRequest;
import com.bikepooling.dto.response.BookerApplicationResponse;
import com.bikepooling.dto.response.DriverRideDetailResponse;
import com.bikepooling.dto.response.RideResponse;
import com.bikepooling.dto.response.RideSearchResponse;
import com.bikepooling.entity.*;
import com.bikepooling.enums.*;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.*;
import com.bikepooling.util.FareUtil;
import com.bikepooling.util.RouteMatchUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RideService {

    private final RideRepository            rideRepo;
    private final RideStatusRepository      rideStatusRepo;
    private final RideApplicationRepository applicationRepo;
    private final UserRepository            userRepo;
    private final VehicleRepository         vehicleRepo;
    private final AdminMetricsService       metricsService;
    private final AppConfigService          configService;
    private final RideAlertService          alertService;
    private final FcmService                fcmService;
    private final OsrmClient               osrmClient;
    private final SosService               sosService;
    private final LiveRideService           liveRideService;

    @Qualifier("osrmExecutor")
    private final Executor osrmExecutor;

    public RideService(
            RideRepository rideRepo, RideStatusRepository rideStatusRepo,
            RideApplicationRepository applicationRepo,
            UserRepository userRepo, VehicleRepository vehicleRepo,
            AdminMetricsService metricsService, AppConfigService configService,
            RideAlertService alertService, FcmService fcmService,
            OsrmClient osrmClient, SosService sosService,
            LiveRideService liveRideService,
            @Qualifier("osrmExecutor") Executor osrmExecutor) {
        this.rideRepo         = rideRepo;
        this.rideStatusRepo   = rideStatusRepo;
        this.applicationRepo  = applicationRepo;
        this.userRepo         = userRepo;
        this.vehicleRepo      = vehicleRepo;
        this.metricsService   = metricsService;
        this.configService    = configService;
        this.alertService     = alertService;
        this.fcmService       = fcmService;
        this.osrmClient       = osrmClient;
        this.sosService       = sosService;
        this.liveRideService  = liveRideService;
        this.osrmExecutor     = osrmExecutor;
    }

    private static final List<RideState> ACTIVE_STATES = List.of(
            RideState.OPEN, RideState.BOOKED, RideState.STARTED
    );

    private static final double MIN_RIDE_DISTANCE_KM = 0.2;
    private static final int    MAX_ACTIVE_RIDES      = 2;
    private static final long   MIN_RETURN_GAP_HOURS  = 1;
    private static final int    DEFAULT_PAGE_SIZE      = 10;
    private static final int    MAX_PAGE_SIZE          = 50;

    // ── Post ride ─────────────────────────────────────────────────────────────

    @Transactional
    public List<RideResponse> postRide(PostRideRequest req, Long userId) {

        User driver = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!driver.isDlVerified()) {
            throw AppException.forbidden(
                    "Driving licence verification is required to post a ride.");
        }

        validateDepartAt(req.getDepartAt());
        if (req.isWantReturnRide()) {
            if (req.getReturnDepartAt() == null) {
                throw AppException.badRequest(
                        "returnDepartAt is required when wantReturnRide is true.");
            }
            validateDepartAt(req.getReturnDepartAt());
            long gapHours = Duration.between(
                    req.getDepartAt(), req.getReturnDepartAt()).toHours();
            if (gapHours < MIN_RETURN_GAP_HOURS) {
                throw AppException.badRequest(
                        "Return ride must depart at least 1 hour after the first ride.");
            }
            if (!req.getReturnDepartAt().isAfter(LocalDateTime.now())) {
                throw AppException.badRequest("Return departure time must be in the future.");
            }
        }

        double roadKm = osrmClient.getRoadDistanceKm(
                req.getFromLat().doubleValue(), req.getFromLng().doubleValue(),
                req.getToLat().doubleValue(),   req.getToLng().doubleValue());
        if (roadKm < MIN_RIDE_DISTANCE_KM) {
            throw AppException.badRequest(
                    "Source and destination are too close. Minimum ride distance is 200 metres.");
        }

        Vehicle vehicle = vehicleRepo.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> AppException.notFound(
                        "Please add a vehicle to your profile first."));
        if (!vehicle.isActive()) {
            throw AppException.badRequest("Your vehicle is currently inactive.");
        }

        long activeCount = rideRepo.countActiveRidesForDriver(userId, ACTIVE_STATES);
        if (activeCount >= MAX_ACTIVE_RIDES) {
            throw AppException.conflict(
                    "You already have " + MAX_ACTIVE_RIDES
                            + " active rides. Complete or cancel one before posting more.");
        }
        if (req.isWantReturnRide() && activeCount + 2 > MAX_ACTIVE_RIDES) {
            throw AppException.conflict(
                    "Not enough active ride slots to post a return ride as well. "
                            + "You have " + activeCount + " active ride(s) already.");
        }

        BigDecimal distanceKm = BigDecimal.valueOf(roadKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal extraKm    = req.getExtraDistanceKm();
        BigDecimal baseFare   = calculateFare(distanceKm);
        PreferredGender gender = resolveGenderPreference(req.getPreferredGender(), driver);

        Ride forwardRide = Ride.builder()
                .postedBy(driver)
                .vehicle(vehicle)
                .fromName(req.getFromName())
                .fromLat(req.getFromLat())
                .fromLng(req.getFromLng())
                .toName(req.getToName())
                .toLat(req.getToLat())
                .toLng(req.getToLng())
                .departAt(req.getDepartAt())
                .fare(baseFare)
                .distanceKm(distanceKm)
                .preferredGender(gender)
                .paymentMode(req.getPaymentMode())
                .routeNotes(req.getRouteNotes())
                .extraDistanceKm(extraKm)
                .returnRide(req.isWantReturnRide())
                .deleted(false)
                .build();

        forwardRide = rideRepo.save(forwardRide);

        RideStatus forwardStatus = rideStatusRepo.save(RideStatus.builder()
                .ride(forwardRide)
                .state(RideState.OPEN)
                .build());

        metricsService.onRidePosted(forwardRide.getDepartAt());
        alertService.matchAlertsForNewRide(forwardRide);

        log.info("Forward ride posted: id={} userId={} dist={}km fare=₹{}",
                forwardRide.getId(), userId, distanceKm, baseFare);

        if (req.isWantReturnRide()) {
            Ride returnRide = Ride.builder()
                    .postedBy(driver)
                    .vehicle(vehicle)
                    .fromName(req.getToName())
                    .fromLat(req.getToLat())
                    .fromLng(req.getToLng())
                    .toName(req.getFromName())
                    .toLat(req.getFromLat())
                    .toLng(req.getFromLng())
                    .departAt(req.getReturnDepartAt())
                    .fare(baseFare)
                    .distanceKm(distanceKm)
                    .preferredGender(gender)
                    .paymentMode(req.getPaymentMode())
                    .routeNotes(req.getRouteNotes())
                    .extraDistanceKm(req.getExtraDistanceKm())
                    .returnRide(true)
                    .deleted(false)
                    .build();

            returnRide = rideRepo.save(returnRide);

            rideStatusRepo.save(RideStatus.builder()
                    .ride(returnRide)
                    .state(RideState.OPEN)
                    .build());

            forwardRide.setReturnRideId(returnRide.getId());
            returnRide.setReturnRideId(forwardRide.getId());
            rideRepo.save(forwardRide);
            rideRepo.save(returnRide);

            metricsService.onRidePosted(returnRide.getDepartAt());
            alertService.matchAlertsForNewRide(returnRide);

            log.info("Return ride posted: id={} paired with forwardRideId={}",
                    returnRide.getId(), forwardRide.getId());

            RideStatus returnStatus = rideStatusRepo.findByRideId(returnRide.getId()).orElse(null);
            return List.of(
                    RideResponse.forDriver(forwardRide, forwardStatus),
                    RideResponse.forDriver(returnRide, returnStatus));
        }

        return List.of(RideResponse.forDriver(forwardRide, forwardStatus));
    }

    // ── Active rides ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DriverRideDetailResponse> getMyActiveRides(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        List<RideState> activeStates = List.of(
                RideState.OPEN, RideState.BOOKED, RideState.STARTED, RideState.VERIFIED);

        List<RideStatus> statuses = user.getRole() == Role.ADMIN
                ? rideStatusRepo.findByStateIn(activeStates)
                : rideStatusRepo.findActiveRidesForUser(userId, activeStates);

        return statuses.stream().map(status -> {
            Ride ride = status.getRide();
            RideApplication confirmedApp = status.getBookedBy() != null
                    ? applicationRepo.findConfirmedByRideId(ride.getId()).orElse(null)
                    : null;

            boolean isDriver = ride.getPostedBy().getId().equals(userId);
            boolean isBooker = status.getBookedBy() != null
                    && status.getBookedBy().getId().equals(userId);

            if (user.getRole() == Role.ADMIN) {
                return DriverRideDetailResponse.from(ride, status, confirmedApp);
            } else if (isDriver) {
                return DriverRideDetailResponse.forDriver(ride, status, confirmedApp);
            } else if (isBooker) {
                return DriverRideDetailResponse.forBooker(ride, status, confirmedApp);
            }
            throw AppException.forbidden("Ride does not belong to the requesting user.");
        }).toList();
    }

    @Transactional(readOnly = true)
    public Page<RideResponse> getDriverRides(Long driverId, int page, int size) {
        Pageable pageable = safePageable(page, size, Sort.by("departAt").descending());
        Page<Ride> ridePage = rideRepo.findDriverRidesPaged(driverId, pageable);

        List<Long> rideIds = ridePage.getContent().stream().map(Ride::getId).toList();
        Map<Long, RideStatus> statusByRideId = rideStatusRepo.findByRideIdIn(rideIds).stream()
                .collect(Collectors.toMap(s -> s.getRide().getId(), s -> s));

        return ridePage.map(ride -> RideResponse.forDriver(ride, statusByRideId.get(ride.getId())));
    }

    @Transactional(readOnly = true)
    public Page<BookerApplicationResponse> getBookerApplications(
            Long bookerId, String filter,
            LocalDateTime from, LocalDateTime to,
            int page, int size) {

        if (from != null && to != null && !to.isAfter(from)) {
            throw AppException.badRequest("Date window 'to' must be after 'from'.");
        }

        List<ApplicationStatus> statuses = switch (filter.toLowerCase()) {
            case "pending"   -> List.of(ApplicationStatus.PENDING);
            case "confirmed" -> List.of(ApplicationStatus.CONFIRMED);
            case "active"    -> List.of(ApplicationStatus.PENDING, ApplicationStatus.CONFIRMED);
            default          -> List.of(ApplicationStatus.PENDING, ApplicationStatus.CONFIRMED,
                    ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN,
                    ApplicationStatus.EXPIRED);
        };

        Pageable pageable = safePageable(page, size, Sort.by("createdAt").descending());
        Page<RideApplication> appPage = applicationRepo
                .findByBookerAndStatusesAndDateRange(bookerId, statuses, from, to, pageable);

        List<Long> confirmedRideIds = appPage.getContent().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.CONFIRMED)
                .map(a -> a.getRide().getId())
                .toList();

        Map<Long, RideStatus> rideStatusByRideId = confirmedRideIds.isEmpty()
                ? Map.of()
                : rideStatusRepo.findByRideIdIn(confirmedRideIds).stream()
                .collect(Collectors.toMap(rs -> rs.getRide().getId(), Function.identity()));

        return appPage.map(app -> {
            if (app.getStatus() != ApplicationStatus.CONFIRMED) {
                return BookerApplicationResponse.from(app, null, null);
            }
            RideStatus rideStatus = rideStatusByRideId.get(app.getRide().getId());
            RideState  rideState  = rideStatus != null ? rideStatus.getState() : null;

            // OTP visible in both BOOKED (pre-posted) and STARTED (live) states
            String otp = (rideStatus != null
                    && (rideStatus.getState() == RideState.BOOKED
                    || rideStatus.getState() == RideState.STARTED))
                    ? rideStatus.getBookerOtp()
                    : null;

            return BookerApplicationResponse.from(app, otp, rideState);
        });
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public Page<RideSearchResponse> searchRides(RideSearchRequest req, Long userId, int page, int size) {

        if (!req.getWindowTo().isAfter(req.getWindowFrom())) {
            throw AppException.badRequest("Window end time must be after window start time.");
        }

        List<Ride> candidates = rideRepo.findOpenRidesInWindow(
                req.getWindowFrom(), req.getWindowTo(), userId);

        List<Ride> stage1Survivors = candidates.stream()
                .filter(ride -> RouteMatchUtil.evaluateStage1(
                        ride.getFromLat().doubleValue(), ride.getFromLng().doubleValue(),
                        ride.getToLat().doubleValue(),   ride.getToLng().doubleValue(),
                        ride.getDistanceKm().doubleValue(),
                        req.getSourceLat().doubleValue(),      req.getSourceLng().doubleValue(),
                        req.getDestinationLat().doubleValue(), req.getDestinationLng().doubleValue()
                ).isMatched())
                .toList();

        List<CompletableFuture<Ride>> futures = stage1Survivors.stream()
                .map(ride -> CompletableFuture.supplyAsync(() -> {
                    OsrmClient.RouteLegs legs = osrmClient.getRouteLegs(
                            new double[]{ride.getFromLat().doubleValue(),
                                    req.getSourceLat().doubleValue(),
                                    req.getDestinationLat().doubleValue(),
                                    ride.getToLat().doubleValue()},
                            new double[]{ride.getFromLng().doubleValue(),
                                    req.getSourceLng().doubleValue(),
                                    req.getDestinationLng().doubleValue(),
                                    ride.getToLng().doubleValue()});
                    double detourKm = Math.max(
                            legs.getTotalKm() - ride.getDistanceKm().doubleValue(), 0.0);
                    boolean matched = RouteMatchUtil.checkDetourBudget(
                            detourKm, ride.getExtraDistanceKm().doubleValue()).isMatched();
                    return matched ? ride : null;
                }, osrmExecutor))
                .toList();

        List<Ride> stage2Survivors = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        List<RideSearchResponse> allMatches =
                stage2Survivors.stream()
                        .map(ride -> {

                            OsrmClient.RouteLegs legs =
                                    osrmClient.getRouteLegs(
                                            new double[]{
                                                    ride.getFromLat().doubleValue(),
                                                    req.getSourceLat().doubleValue(),
                                                    req.getDestinationLat().doubleValue(),
                                                    ride.getToLat().doubleValue()
                                            },
                                            new double[]{
                                                    ride.getFromLng().doubleValue(),
                                                    req.getSourceLng().doubleValue(),
                                                    req.getDestinationLng().doubleValue(),
                                                    ride.getToLng().doubleValue()
                                            }
                                    );

                            double riderKm = legs.getLeg(1);

                            BigDecimal estimatedFare =
                                    FareUtil.calculateBookerFare(
                                            riderKm,
                                            ride.getDistanceKm().doubleValue(),
                                            ride.getFare(),
                                            configService.getMinFare()
                                    );

                            return RideSearchResponse.from(
                                    ride,
                                    estimatedFare
                            );

                        })
                        .toList();
        Pageable pageable = safePageable(page, size, Sort.by("departAt").ascending());
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), allMatches.size());
        List<RideSearchResponse> pageContent =
                start >= allMatches.size()
                        ? List.of()
                        : allMatches.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allMatches.size());
    }

    // ── Update ride ───────────────────────────────────────────────────────────

    @Transactional
    public RideResponse updateRide(Long rideId, UpdateRideRequest req, Long userId) {

        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));
        Ride ride = status.getRide();

        if (!ride.getPostedBy().getId().equals(userId)) {
            throw AppException.forbidden("You can only edit your own ride.");
        }
        if (status.getState() != RideState.OPEN) {
            throw AppException.conflict(
                    "Ride can only be edited before it is booked. State: " + status.getState());
        }
        if (req.getExtraDistanceKm() == null && req.getPaymentMode() == null
                && req.getRouteNotes() == null && req.getPreferredGender() == null) {
            throw AppException.badRequest("No fields provided for update.");
        }

        User driver = ride.getPostedBy();
        if (req.getExtraDistanceKm() != null) ride.setExtraDistanceKm(req.getExtraDistanceKm());
        if (req.getPaymentMode()     != null) ride.setPaymentMode(req.getPaymentMode());
        if (req.getRouteNotes()      != null) ride.setRouteNotes(req.getRouteNotes());
        if (req.getPreferredGender() != null) {
            ride.setPreferredGender(resolveGenderPreference(req.getPreferredGender(), driver));
        }

        ride = rideRepo.save(ride);
        log.info("Ride updated: id={} by userId={}", rideId, userId);
        return RideResponse.forDriver(ride, status);
    }

    // ── Start ride (pre-posted flow only: BOOKED → STARTED) ──────────────────

    @Transactional
    public void startRide(Long rideId, Long userId) {
        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        if (!status.getRide().getPostedBy().getId().equals(userId)) {
            throw AppException.forbidden("Only the driver can start the ride.");
        }
        if (status.getState() == RideState.OPEN) {
            throw AppException.conflict("Ride cannot be started without a confirmed booking.");
        }
        if (status.getState() != RideState.BOOKED) {
            throw AppException.conflict(
                    "Ride cannot be started. Current state: " + status.getState());
        }

        LocalDateTime departAt    = status.getRide().getDepartAt();
        LocalDateTime now         = LocalDateTime.now();
        LocalDateTime windowStart = departAt.minusMinutes(30);
        LocalDateTime windowEnd   = departAt.plusMinutes(30);

        if (now.isBefore(windowStart)) {
            throw AppException.conflict(
                    "Too early to start. You can start from " + windowStart + ".");
        }
        if (now.isAfter(windowEnd)) {
            throw AppException.conflict(
                    "Departure window has passed (" + departAt + "). Please cancel and repost.");
        }

        status.setState(RideState.STARTED);
        status.setStartedAt(now);
        rideStatusRepo.save(status);

        // Seed Redis so location tracking and dwell checks work for the pre-posted flow
        // exactly as they do for the live flow once STARTED.
        RideApplication confirmedApp = applicationRepo
                .findConfirmedByRideId(rideId)
                .orElse(null);

        if (confirmedApp != null) {
            liveRideService.seedRedisForStartedRide(
                    rideId,
                    userId,
                    confirmedApp.getPickupLat().doubleValue(),  // ← new
                    confirmedApp.getPickupLng().doubleValue(),  // ← new
                    confirmedApp.getDropLat().doubleValue(),
                    confirmedApp.getDropLng().doubleValue()
            );
        } else {
            log.warn("startRide: no confirmed application found for rideId={}, " +
                    "Redis not seeded — dwell check will not auto-complete.", rideId);
        }

        User booker = status.getBookedBy();
        if (booker != null) {
            fcmService.notifyBookerRideStarted(
                    booker.getId(),
                    status.getRide().getPostedBy().getFullName(),
                    rideId);
        }
        log.info("Ride started (pre-posted flow): rideId={}", rideId);
    }

    // ── OTP ───────────────────────────────────────────────────────────────────

    public void generateBookingOtp(Long rideId) {
        RideStatus status = rideStatusRepo.findByRideId(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));
        String otp = String.format("%04d", new java.security.SecureRandom().nextInt(10_000));
        status.setBookerOtp(otp);
        rideStatusRepo.save(status);
        log.info("OTP generated for rideId={}", rideId);
    }

    @Transactional(readOnly = true)
    public String getBookerOtp(Long rideId, Long userId) {
        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));
        if (status.getBookedBy() == null || !status.getBookedBy().getId().equals(userId)) {
            throw AppException.forbidden("OTP is only visible to the confirmed booker.");
        }
        if (status.getBookerOtp() == null) {
            throw AppException.notFound("No OTP found for this ride.");
        }
        return status.getBookerOtp();
    }

    /**
     * OTP verification — works for both flows:
     *
     * Pre-posted flow: state = STARTED  (driver started manually, heading to pickup)
     * Live flow:       state = STARTED  (driver confirmed booker while already moving)
     *
     * Both flows land in STARTED before OTP verify, so a single check is enough.
     * After verify → VERIFIED; dwell check auto-completes → COMPLETED.
     */
    @Transactional
    public void verifyRideOtp(Long rideId, String submittedOtp, Long userId) {
        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        if (status.getBookedBy() == null
                || !status.getRide().getPostedBy().getId().equals(userId)) {
            throw AppException.forbidden("Only the driver can verify the OTP.");
        }

        // Both flows are in STARTED state at OTP time
        if (status.getState() != RideState.STARTED) {
            throw AppException.conflict(
                    "OTP can only be verified while the ride is in STARTED state. " +
                            "Current state: " + status.getState());
        }
        if (status.getVerifiedAt() != null) {
            throw AppException.conflict("OTP has already been verified for this ride.");
        }
        if (status.getBookerOtp() == null || !status.getBookerOtp().equals(submittedOtp)) {
            throw AppException.badRequest("Invalid OTP.");
        }

        status.setState(RideState.VERIFIED);
        status.setVerifiedAt(LocalDateTime.now());
        rideStatusRepo.save(status);

        // Reset dwell tracking in Redis for the new VERIFIED destination check
        liveRideService.onRideVerified(rideId);

        log.info("OTP verified → VERIFIED: rideId={} by driverId={}", rideId, userId);
    }

    // ── Complete ride ─────────────────────────────────────────────────────────

    /**
     * Manual completion — fallback for when auto-complete (dwell check) doesn't trigger.
     *
     * Pre-posted flow: driver can call this from STARTED (if OTP not yet verified — emergency)
     *                  or VERIFIED (normal manual complete)
     * Live flow:       normally auto-completes from VERIFIED via dwell;
     *                  this is the fallback if driver's app crashes, etc.
     */
    @Transactional
    public void completeRide(Long rideId, Long userId) {
        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        if (!status.getRide().getPostedBy().getId().equals(userId)) {
            throw AppException.forbidden("Only the driver can complete the ride.");
        }

        boolean isVerified = status.getState() == RideState.VERIFIED;

        if (!isVerified) {
            throw AppException.conflict(
                    "Ride must be in VERIFIED state to complete. " +
                            "Current state: " + status.getState());
        }

        // For STARTED state, require OTP to have been verified (pre-posted normal flow)
        // For VERIFIED state, OTP was already verified — allow direct completion

        if (status.getVerifiedAt() == null) {
            throw AppException.conflict(
                    "Booker has not verified the OTP yet. " +
                            "Please ask the booker to verify before completing.");
        }
        RideApplication application = applicationRepo.findConfirmedByRideId(rideId)
                .orElseThrow(() -> AppException.notFound("No confirmed booker"));
        application.setStatus(ApplicationStatus.FINISH);
        applicationRepo.save(application);

        status.setState(RideState.COMPLETED);
        status.setCompletedAt(LocalDateTime.now());
        rideStatusRepo.save(status);

        sosService.resolveActiveSosForRide(rideId);

        User booker = status.getBookedBy();
        if (booker != null) {
            fcmService.notifyBookerRideCompleted(
                    booker.getId(), status.getRide().getPostedBy().getFullName(), rideId);
        }

        BigDecimal actualFareCharged = applicationRepo.findConfirmedByRideId(rideId)
                .map(RideApplication::getBookerFare)
                .orElse(status.getRide().getFare());

        metricsService.onRideCompleted(actualFareCharged);
        log.info("Ride manually completed: rideId={} from state={}", rideId,
                isVerified ? "VERIFIED" : "STARTED");
    }

    // ── Cancel ride ───────────────────────────────────────────────────────────

    @Transactional
    public void cancelRide(Long rideId, Long userId) {
        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found"));

        boolean isDriver = status.getRide().getPostedBy().getId().equals(userId);
        boolean isBooker = status.getBookedBy() != null
                && status.getBookedBy().getId().equals(userId);

        if (!isDriver && !isBooker) {
            throw AppException.forbidden("Only the driver or confirmed booker can cancel.");
        }
        if (status.getState() == RideState.COMPLETED) {
            throw AppException.conflict("Cannot cancel a completed ride.");
        }
        if (status.getState() == RideState.STARTED && !isBooker) {
            throw AppException.conflict("Driver cannot cancel a ride that has already started.");
        }

        boolean alreadyCancelled = status.getState() == RideState.CANCELLED;
        if (alreadyCancelled && isDriver) {
            throw AppException.conflict("Ride is already cancelled.");
        }

        applicationRepo.findConfirmedByRideId(rideId).ifPresent(app -> {
            app.setStatus(ApplicationStatus.WITHDRAWN);
            app.setDeleted(true);
            app.setDeletedAt(LocalDateTime.now());
            applicationRepo.save(app);
        });

        if (isDriver) {
            applicationRepo.expireApplicationsForRide(rideId, LocalDateTime.now());
        }

        if (alreadyCancelled) {
            log.info("Booker userId={} cleaned up orphaned application on cancelled rideId={}",
                    userId, rideId);
            return;
        }

        status.setState(RideState.CANCELLED);
        status.setCancelledAt(LocalDateTime.now());
        rideStatusRepo.save(status);

        String driverName = status.getRide().getPostedBy().getFullName();
        if (isDriver && status.getBookedBy() != null) {
            fcmService.notifyBookerRideCancelled(
                    status.getBookedBy().getId(), driverName, rideId);
        }
        if (isBooker) {
            fcmService.notifyDriverBookerCancelled(
                    status.getRide().getPostedBy().getId(),
                    status.getBookedBy().getFullName(), rideId);
        }

        metricsService.onRideCancelled();
        log.info("Ride cancelled: rideId={} by userId={}", rideId, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateDepartAt(LocalDateTime departAt) {
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate rideDate = departAt.toLocalDate();
        if (!rideDate.equals(today) && !rideDate.equals(tomorrow)) {
            throw AppException.badRequest("Rides can only be posted for today or tomorrow.");
        }
    }

    private PreferredGender resolveGenderPreference(PreferredGender requested, User driver) {
        if (requested == null || requested == PreferredGender.ANY) return PreferredGender.ANY;
        if (driver.getGender() == Gender.FEMALE) return requested;
        throw AppException.badRequest("Gender preference can only be set by female drivers.");
    }

    private BigDecimal calculateFare(BigDecimal distanceKm) {
        BigDecimal farePerKm = configService.getFarePerKm();
        BigDecimal minFare   = configService.getMinFare();
        BigDecimal fare      = distanceKm.multiply(farePerKm).setScale(2, RoundingMode.HALF_UP);
        return fare.compareTo(minFare) < 0 ? minFare : fare;
    }

    private Pageable safePageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = (size > 0 && size <= MAX_PAGE_SIZE) ? size : DEFAULT_PAGE_SIZE;
        return PageRequest.of(safePage, safeSize, sort);
    }
}