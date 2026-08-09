package com.bikepooling.entity;

import com.bikepooling.enums.PaymentMode;
import com.bikepooling.enums.PreferredGender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by", nullable = false)
    private User postedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

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

    @Column(nullable = false)
    private LocalDateTime departAt;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fare;

    @Column(precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private PreferredGender preferredGender = PreferredGender.ANY;

    @Column(columnDefinition = "TEXT")
    private String routeNotes;

    @Column(name = "extra_distance_km", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal extraDistanceKm = BigDecimal.ZERO;

    @Column(name = "return_ride_id")
    private Long returnRideId;

    @Column(name = "is_return_ride", nullable = false)
    @Builder.Default
    private boolean returnRide = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}