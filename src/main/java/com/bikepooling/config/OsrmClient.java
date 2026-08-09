package com.bikepooling.config;

import com.bikepooling.util.GeoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for the OSRM routing API.
 *
 * Uses the Spring-managed ObjectMapper (from JacksonConfig) so that any
 * global Jackson configuration (JavaTimeModule, custom serializers, etc.)
 * is consistently applied here too.
 *
 * Falls back to Haversine straight-line distance if OSRM is unreachable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OsrmClient {

    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/driving/";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Injected — shares configuration with the rest of the application
    private final ObjectMapper objectMapper;

    public double getRoadDistanceKm(double fromLat, double fromLng,
                                    double toLat,   double toLng) {
        try {
            String url = buildUrl(fromLat, fromLng, toLat, toLng);
            JsonNode root = fetch(url);

            if (!"Ok".equals(root.path("code").asText())) {
                log.warn("OSRM returned non-OK code: {}", root.path("code").asText());
                return haversineKm(fromLat, fromLng, toLat, toLng);
            }

            double distanceMetres = root
                    .path("routes").path(0)
                    .path("distance").asDouble();
            return distanceMetres / 1000.0;

        } catch (Exception e) {
            log.warn("OSRM distance call failed, falling back to Haversine. Error: {}", e.getMessage());
            return haversineKm(fromLat, fromLng, toLat, toLng);
        }
    }

    public int getRoadDurationMinutes(double fromLat, double fromLng,
                                      double toLat,   double toLng) {
        try {
            String url = buildUrl(fromLat, fromLng, toLat, toLng);
            JsonNode root = fetch(url);

            double durationSeconds = root
                    .path("routes").path(0)
                    .path("duration").asDouble();
            return (int) Math.ceil(durationSeconds / 60.0);

        } catch (Exception e) {
            log.warn("OSRM duration call failed. Error: {}", e.getMessage());
            return 0;
        }
    }

    public RouteLegs getRouteLegs(double[] lats, double[] lngs) {
        if (lats.length != lngs.length || lats.length < 2) {
            throw new IllegalArgumentException(
                    "Need at least 2 waypoints with matching lat/lng arrays.");
        }
        try {
            StringBuilder coords = new StringBuilder();
            for (int i = 0; i < lats.length; i++) {
                if (i > 0) coords.append(';');
                coords.append(lngs[i]).append(',').append(lats[i]);
            }
            String url = OSRM_BASE + coords + "?overview=false";
            JsonNode root = fetch(url);

            if (!"Ok".equals(root.path("code").asText())) {
                log.warn("OSRM returned non-OK code for multi-leg route: {}",
                        root.path("code").asText());
                return haversineLegs(lats, lngs);
            }

            JsonNode legsNode = root.path("routes").path(0).path("legs");
            if (legsNode.size() != lats.length - 1) {
                throw new IllegalStateException(
                        "Unexpected OSRM legs count: " + legsNode.size());
            }

            double[] legKm = new double[lats.length - 1];
            for (int i = 0; i < legKm.length; i++) {
                legKm[i] = legsNode.path(i).path("distance").asDouble() / 1000.0;
            }
            double totalKm = root.path("routes").path(0).path("distance").asDouble() / 1000.0;
            return new RouteLegs(totalKm, legKm);

        } catch (Exception e) {
            log.warn("OSRM multi-leg call failed, falling back to Haversine. Error: {}",
                    e.getMessage());
            return haversineLegs(lats, lngs);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildUrl(double fromLat, double fromLng,
                            double toLat,   double toLng) {
        return OSRM_BASE
                + fromLng + "," + fromLat + ";"
                + toLng   + "," + toLat
                + "?overview=false";
    }

    private JsonNode fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private RouteLegs haversineLegs(double[] lats, double[] lngs) {
        double[] legKm = new double[lats.length - 1];
        double total = 0;
        for (int i = 0; i < legKm.length; i++) {
            legKm[i] = haversineKm(lats[i], lngs[i], lats[i + 1], lngs[i + 1]);
            total += legKm[i];
        }
        return new RouteLegs(total, legKm);
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        return GeoUtil.distanceKm(lat1, lng1, lat2, lng2);
    }

    // ── RouteLegs ─────────────────────────────────────────────────────────────

    public static final class RouteLegs {
        private final double   totalKm;
        private final double[] legKm;

        public RouteLegs(double totalKm, double[] legKm) {
            this.totalKm = totalKm;
            this.legKm   = legKm;
        }

        public double   getTotalKm()        { return totalKm; }
        public double[] getLegKm()          { return legKm;   }
        public double   getLeg(int index)   { return legKm[index]; }
    }
}