package com.bikepooling.entity;

import com.bikepooling.enums.RideState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ride_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RideState state;

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

    @Column(name = "pre_departure_notified_at")
    private LocalDateTime preDepartureNotifiedAt;

    private Integer etaMinutes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}