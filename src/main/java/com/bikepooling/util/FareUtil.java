package com.bikepooling.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for the booker-fare formula:
 *   bookerFare = (bookerKm / driverKm) x driverFare, floored at minFare.
 * Used by RideService.searchRides (estimate), RideApplicationService.apply
 * (locked-in fare), and ScheduledRideApplicationService.apply (locked-in fare)
 * -- do not reimplement this calculation anywhere else.
 */
public final class FareUtil {

    private FareUtil() {}

    public static BigDecimal calculateBookerFare(double bookerKm, double driverKm,
                                                 BigDecimal driverFare, BigDecimal minFare) {
        if (driverKm <= 0) return minFare;
        double ratio = Math.min(bookerKm / driverKm, 1.0);
        BigDecimal fare = driverFare.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
        return fare.compareTo(minFare) < 0 ? minFare : fare;
    }
}