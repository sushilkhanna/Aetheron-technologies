package com.bikepooling.entity;

import com.bikepooling.enums.SosStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sos_alerts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SosAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_token", nullable = false, unique = true, length = 36)
    @Builder.Default
    private String trackingToken = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private ScheduledRideInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id", nullable = false)
    private User triggeredBy;

    @Column(name = "triggered_by_role", nullable = false, length = 20)
    private String triggeredByRole;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SosStatus status = SosStatus.TRIGGERED;

    @Column(name = "showed_call_112_option", nullable = false)
    @Builder.Default
    private boolean showedCall112Option = false;

    @Column(name = "contact_sms_sent", nullable = false)
    @Builder.Default
    private boolean contactSmsSent = false;

    @Column(name = "admin_sms_sent", nullable = false)
    @Builder.Default
    private boolean adminSmsSent = false;

    @Column(name = "counterpart_notified", nullable = false)
    @Builder.Default
    private boolean counterpartNotified = false;

    private LocalDateTime triggeredAt;
    private LocalDateTime lastPingAt;
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}