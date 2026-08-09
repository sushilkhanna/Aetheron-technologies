package com.bikepooling.entity;

import com.bikepooling.enums.RideState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;

/**
 * One bookable day generated from a ScheduledRideTemplate.
 * This is the actual state-machine unit — mirrors Ride+RideStatus's
 * OPEN → BOOKED → STARTED → VERIFIED → COMPLETED flow, just scoped to
 * a single calendar date instead of a one-off Ride.
 *
 * departTime and extraDistanceKm are snapshotted from the template at
 * creation time so that template edits only affect instances still OPEN
 * (see ScheduledRideService.updateScheduledRide) without disturbing
 * already-booked days.
 */
@Entity
@Table(
        name = "scheduled_ride_instances",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_instance_template_date",
                columnNames = {"template_id", "ride_date"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduledRideInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ScheduledRideTemplate template;

    @Column(name = "ride_date", nullable = false)
    private LocalDate rideDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    /** Snapshotted from template at creation/last-open-update — see class javadoc. */
    @Column(nullable = false)
    private LocalTime departTime;

    @Column(name = "extra_distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal extraDistanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RideState state = RideState.OPEN;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by")
    private User bookedBy;

    @Column(name = "booker_otp", length = 4)
    private String bookerOtp;

    private LocalDateTime bookedAt;
    private LocalDateTime startedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}