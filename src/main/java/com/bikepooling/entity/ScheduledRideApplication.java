package com.bikepooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One booker's application to a scheduled ride template — fixed pickup/drop
 * for all days they select, since a booker's route doesn't usually change
 * day to day. Individual day selections live in ScheduledRideApplicationDay.
 */
@Entity
@Table(name = "scheduled_ride_applications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduledRideApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ScheduledRideTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booker_id", nullable = false)
    private User booker;

    @Column(name = "pickup_name", length = 255)
    private String pickupName;

    @Column(name = "pickup_lat", precision = 10, scale = 7)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", precision = 10, scale = 7)
    private BigDecimal pickupLng;

    @Column(name = "drop_name", length = 255)
    private String dropName;

    @Column(name = "drop_lat", precision = 10, scale = 7)
    private BigDecimal dropLat;

    @Column(name = "drop_lng", precision = 10, scale = 7)
    private BigDecimal dropLng;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "booker_distance_km", precision = 8, scale = 2)
    private BigDecimal bookerDistanceKm;

    @Column(name = "booker_fare", precision = 8, scale = 2)
    private BigDecimal bookerFare;

    @OneToMany(mappedBy = "application", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScheduledRideApplicationDay> days = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}