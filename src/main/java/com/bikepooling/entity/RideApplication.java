package com.bikepooling.entity;

import com.bikepooling.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ride_applications",
        indexes = {
                @Index(name = "idx_application_ride",   columnList = "ride_id"),
                @Index(name = "idx_application_booker", columnList = "booker_id"),
                @Index(name = "idx_application_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booker_id", nullable = false)
    private User booker;

    // booker's requested pickup point
    @Column(name = "pickup_name", length = 255)
    private String pickupName;

    @Column(name = "pickup_lat", precision = 10, scale = 7)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", precision = 10, scale = 7)
    private BigDecimal pickupLng;

    // booker's requested drop point
    @Column(name = "drop_name", length = 255)
    private String dropName;

    @Column(name = "drop_lat", precision = 10, scale = 7)
    private BigDecimal dropLat;

    @Column(name = "drop_lng", precision = 10, scale = 7)
    private BigDecimal dropLng;

    @Column(columnDefinition = "TEXT")
    private String note;

    // ── fare fields ───────────────────────────────────────────────────────────

    /**
     * Road distance of the booker's actual pickup → drop route in km.
     * Calculated at apply time via OSRM (Haversine fallback).
     */
    @Column(name = "booker_distance_km", precision = 8, scale = 2)
    private BigDecimal bookerDistanceKm;

    /**
     * Final fare the booker will pay.
     * = (bookerDistanceKm / driverDistanceKm) × driverBaseFare
     * Always >= platform minimum fare.
     * Locked in at application time — driver sees this before confirming.
     */
    @Column(name = "booker_fare", precision = 8, scale = 2)
    private BigDecimal bookerFare;

    // ── status / soft-delete ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}