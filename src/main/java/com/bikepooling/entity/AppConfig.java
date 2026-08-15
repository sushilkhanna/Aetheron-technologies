package com.bikepooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin-controlled platform configuration.
 * Single row — always fetch by id = 1.
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
     */
    @Column(name = "match_radius_metres", nullable = false)
    private Integer matchRadiusMetres;

    /**
     * Time window in minutes for departure time matching (± this value).
     */
    @Column(name = "match_time_window_minutes", nullable = false)
    private Integer matchTimeWindowMinutes;

    // Launch Management Configs
    @Builder.Default
    @Column(name = "launch_mode", nullable = false, length = 30)
    private String launchMode = "COMING_SOON"; // COMING_SOON vs LIVE_LAUNCHED

    @Column(name = "launch_target_date_time")
    private LocalDateTime launchTargetDateTime;

    @Column(name = "android_app_url", length = 500)
    private String androidAppUrl;

    @Column(name = "ios_app_url", length = 500)
    private String iosAppUrl;

    @Column(name = "launch_message", length = 500)
    private String launchMessage;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}