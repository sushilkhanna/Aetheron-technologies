package com.bikepooling.service;

import com.bikepooling.dto.request.ApplyRideRequest;
import com.bikepooling.dto.response.RideApplicationResponse;
import com.bikepooling.entity.RideApplication;
import com.bikepooling.entity.RideStatus;
import com.bikepooling.entity.User;
import com.bikepooling.enums.ApplicationStatus;
import com.bikepooling.enums.RideState;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.RideApplicationRepository;
import com.bikepooling.repository.RideStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideApplicationWriter {

    private final RideStatusRepository      rideStatusRepo;
    private final RideApplicationRepository applicationRepo;

    /**
     * The only DB write in the apply flow. Called only after OSRM and fare
     * math are already done, so this transaction is short and never waits
     * on the network — re-checks ride state fresh, since time has passed
     * since the caller's first check.
     */
    @Transactional
    public RideApplicationResponse save(Long rideId, User booker, ApplyRideRequest req,
                                        BigDecimal bookerDistanceKm, BigDecimal bookerFare) {

        RideStatus status = rideStatusRepo.findByRideIdWithDetails(rideId)
                .orElseThrow(() -> AppException.notFound("Ride not found: " + rideId));

        if (status.getState() != RideState.OPEN) {
            throw AppException.conflict("This ride is no longer open. State: " + status.getState());
        }

        RideApplication application = RideApplication.builder()
                .ride(status.getRide())
                .booker(booker)
                .pickupName(req.getPickupName())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .dropName(req.getDropName())
                .dropLat(req.getDropLat())
                .dropLng(req.getDropLng())
                .note(req.getNote())
                .bookerDistanceKm(bookerDistanceKm)
                .bookerFare(bookerFare)
                .status(ApplicationStatus.PENDING)
                .deleted(false)
                .build();

        application = applicationRepo.save(application);
        log.info("Application persisted: id={} rideId={} bookerId={}",
                application.getId(), rideId, booker.getId());

        return RideApplicationResponse.from(application);
    }
}