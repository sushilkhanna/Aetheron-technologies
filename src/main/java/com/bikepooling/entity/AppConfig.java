package com.bikepooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin-controlled platform configuration.
 * Single row — always fetch by id = 1.
 * Admin can update these via a future /api/admin/config endpoint.
 */
@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    @Id
    private Long id; // always 1 — single row table

    /**
     * Fare charged per kilometre (e.g. 3.00 = ₹3/km).
     * Used in RideService to calculate ride fare = distanceKm × farePerKm.
     */
    @Column(name = "fare_per_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal farePerKm;

    /**
     * Minimum fare for any ride regardless of distance (e.g. ₹10).
     */
    @Column(name = "min_fare", nullable = false, precision = 6, scale = 2)
    private BigDecimal minFare;

    /**
     * Maximum radius in metres for source/destination matching.
     * Default 500m. Admin can tighten or loosen this.
     */
    @Column(name = "match_radius_metres", nullable = false)
    private Integer matchRadiusMetres;

    /**
     * Time window in minutes for departure time matching (± this value).
     * Default 30 mins.
     */
    @Column(name = "match_time_window_minutes", nullable = false)
    private Integer matchTimeWindowMinutes;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}