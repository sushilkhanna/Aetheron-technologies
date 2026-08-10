package com.bikepooling.entity;

import com.bikepooling.enums.PaymentMode;
import com.bikepooling.enums.PreferredGender;
import com.bikepooling.enums.ScheduledRideStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "scheduled_ride_templates")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduledRideTemplate {

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
    private LocalTime departTime;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fare;

    @Column(name = "extra_distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal extraDistanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private PreferredGender preferredGender = PreferredGender.ANY;

    @Column(columnDefinition = "TEXT")
    private String routeNotes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "scheduled_ride_template_dates",
            joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "ride_date", nullable = false)
    @Builder.Default
    private Set<LocalDate> dates = new HashSet<>();

    /** First bookable date selected. */
    @Column(name = "week_start")
    private LocalDate weekStart;

    /** Last bookable date selected. */
    @Column(name = "week_end")
    private LocalDate weekEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduledRideStatus status = ScheduledRideStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}