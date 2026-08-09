package com.bikepooling.util;


public final class RouteMatchUtil {

    private static final double OFF_ROUTE_MIN_KM = 0.5;
    private static final double OFF_ROUTE_MAX_KM = 2.0;
    private static final double OFF_ROUTE_PCT     = 0.10;

    public static final double MIN_EXTRA_DIST_KM = 0.2;

    private RouteMatchUtil() {}

    public static double maxOffRouteKm(double routeDistanceKm) {
        return Math.min(Math.max(routeDistanceKm * OFF_ROUTE_PCT, OFF_ROUTE_MIN_KM), OFF_ROUTE_MAX_KM);
    }

    public static MatchResult evaluateStage1(double aLat, double aLng,
                                             double bLat, double bLng,
                                             double routeDistanceKm,
                                             double pLat, double pLng,
                                             double dLat, double dLng) {

        double tolerance = maxOffRouteKm(routeDistanceKm);

        GeoUtil.RouteProjection pickupProj = GeoUtil.projectOntoRoute(aLat, aLng, bLat, bLng, pLat, pLng);
        GeoUtil.RouteProjection dropProj   = GeoUtil.projectOntoRoute(aLat, aLng, bLat, bLng, dLat, dLng);

        if (pickupProj.getOffRouteKm() > tolerance) {
            return MatchResult.rejected(String.format(
                    "Pickup is %.1f km off the route — beyond the %.1f km allowed for a %.1f km ride.",
                    pickupProj.getOffRouteKm(), tolerance, routeDistanceKm));
        }
        if (dropProj.getOffRouteKm() > tolerance) {
            return MatchResult.rejected(String.format(
                    "Drop is %.1f km off the route — beyond the %.1f km allowed for a %.1f km ride.",
                    dropProj.getOffRouteKm(), tolerance, routeDistanceKm));
        }

        double minAlong = -tolerance;
        double maxAlong = routeDistanceKm + tolerance;

        if (pickupProj.getAlongTrackKm() < minAlong || pickupProj.getAlongTrackKm() > maxAlong) {
            return MatchResult.rejected("Pickup point is too far before the route start or beyond the route end.");
        }
        if (dropProj.getAlongTrackKm() < minAlong || dropProj.getAlongTrackKm() > maxAlong) {
            return MatchResult.rejected("Drop point is too far before the route start or beyond the route end.");
        }
        if (pickupProj.getAlongTrackKm() >= dropProj.getAlongTrackKm()) {
            return MatchResult.rejected("Pickup and drop direction does not match the driver's route.");
        }

        return MatchResult.matched(0.0);
    }

    public static MatchResult checkDetourBudget(double detourKm, double maxExtraKm) {
        double allowedKm = Math.max(maxExtraKm, MIN_EXTRA_DIST_KM);
        if (detourKm > allowedKm) {
            return MatchResult.rejected(String.format(
                    "This ride needs a %.1f km detour — beyond the driver's %.1f km limit.",
                    detourKm, allowedKm));
        }
        return MatchResult.matched(detourKm);
    }

    public static final class MatchResult {
        private final boolean matched;
        private final double  detourKm;
        private final String  reason;

        private MatchResult(boolean matched, double detourKm, String reason) {
            this.matched = matched;
            this.detourKm = detourKm;
            this.reason = reason;
        }

        public static MatchResult matched(double detourKm)  { return new MatchResult(true, detourKm, null); }
        public static MatchResult rejected(String reason)   { return new MatchResult(false, -1, reason); }

        public boolean isMatched()   { return matched; }
        public double  getDetourKm() { return detourKm; }
        public String  getReason()   { return reason; }
    }
}