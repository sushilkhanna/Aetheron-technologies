package com.bikepooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ride_requests",
        indexes = {
                @Index(name = "idx_rr_booker", columnList = "booker_id"),
                @Index(name = "idx_rr_active", columnList = "active, expires_at"),
                @Index(name = "idx_rr_depart", columnList = "depart_from, depart_to")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booker_id", nullable = false)
    private User booker;

    @Column(name = "pickup_name", length = 255)
    private String pickupName;

    @Column(name = "pickup_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal pickupLng;

    @Column(name = "drop_name", length = 255)
    private String dropName;

    @Column(name = "drop_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal dropLat;

    @Column(name = "drop_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal dropLng;

    @Column(name = "depart_from", nullable = false)
    private LocalDateTime departFrom;

    @Column(name = "depart_to", nullable = false)
    private LocalDateTime departTo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}