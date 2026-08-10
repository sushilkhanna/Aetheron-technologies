package com.bikepooling.entity;

import com.bikepooling.enums.LiveRideState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_rides")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LiveRide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booker_id")
    private User booker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    // Driver's full planned route
    @Column(nullable = false, length = 255)
    private String fromName;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal fromLat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal fromLng;

    @Column(nullable = false, length = 255)
    private String toName;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal toLat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal toLng;

    // Booker's specific segment (if booked)
    @Column(length = 255)
    private String pickupName;

    @Column(precision = 10, scale = 7)
    private BigDecimal pickupLat;

    @Column(precision = 10, scale = 7)
    private BigDecimal pickupLng;

    @Column(length = 255)
    private String dropName;

    @Column(precision = 10, scale = 7)
    private BigDecimal dropLat;

    @Column(precision = 10, scale = 7)
    private BigDecimal dropLng;

    @Column(precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(precision = 8, scale = 2)
    private BigDecimal fare;

    @Column(name = "extra_distance_km", precision = 6, scale = 2)
    private BigDecimal extraDistanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveRideState state;

    @Column(length = 10)
    private String bookerOtp;

    private LocalDateTime startedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
