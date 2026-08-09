package com.bikepooling.entity;

import com.bikepooling.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Links one ScheduledRideApplication to one specific ScheduledRideInstance
 * (i.e. one selected day). This is the row that actually gets
 * confirmed/rejected/cancelled — confirming Tuesday only touches the
 * Tuesday link, never the application as a whole.
 */
@Entity
@Table(
        name = "scheduled_ride_application_days",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_app_instance",
                columnNames = {"application_id", "instance_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduledRideApplicationDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ScheduledRideApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private ScheduledRideInstance instance;

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