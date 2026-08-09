package com.bikepooling.util;

/**
 * Geographic utility methods.
 * Single source of truth — no duplicate Haversine across services.
 */
public final class GeoUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double KM_PER_DEG_LAT   = 110.574;

    private GeoUtil() {}

    /**
     * @deprecated independent circle checks don't account for route direction —
     * use {@link RouteMatchUtil} for pickup/drop matching against a ride's route.
     */
    @Deprecated
    public static boolean isWithinRadius(double lat1, double lng1,
                                         double lat2, double lng2,
                                         double radiusKm) {
        return distanceKm(lat1, lng1, lat2, lng2) <= radiusKm;
    }

    /** Distance in km between two lat/lng points (Haversine, crow-fly). */
    public static double distanceKm(double lat1, double lng1,
                                    double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Projects point P onto the line through A and B using a local flat-earth
     * (equirectangular) approximation centred at A. Error is well under 1% for
     * route lengths under ~50km.
     *
     * t            - 0 at A, 1 at B; negative before A, greater than 1 past B
     * offRouteKm   - perpendicular distance from P to the line (always >= 0)
     * alongTrackKm - distance from A, along the line, to P's projected foot
     */
    public static RouteProjection projectOntoRoute(double aLat, double aLng,
                                                   double bLat, double bLng,
                                                   double pLat, double pLng) {

        double kmPerDegLng = 111.320 * Math.cos(Math.toRadians(aLat));

        double bx = (bLng - aLng) * kmPerDegLng;
        double by = (bLat - aLat) * KM_PER_DEG_LAT;
        double px = (pLng - aLng) * kmPerDegLng;
        double py = (pLat - aLat) * KM_PER_DEG_LAT;

        double abLenSq = bx * bx + by * by;
        if (abLenSq < 1e-9) {
            return new RouteProjection(0.0, Math.sqrt(px * px + py * py), 0.0);
        }

        double t      = (px * bx + py * by) / abLenSq;
        double cross  = px * by - py * bx;
        double abLen  = Math.sqrt(abLenSq);

        return new RouteProjection(t, Math.abs(cross) / abLen, t * abLen);
    }

    public static final class RouteProjection {
        private final double t;
        private final double offRouteKm;
        private final double alongTrackKm;

        public RouteProjection(double t, double offRouteKm, double alongTrackKm) {
            this.t = t;
            this.offRouteKm = offRouteKm;
            this.alongTrackKm = alongTrackKm;
        }

        public double getT()            { return t; }
        public double getOffRouteKm()   { return offRouteKm; }
        public double getAlongTrackKm() { return alongTrackKm; }
    }
}