package com.bikepooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ride_alerts",
        indexes = {
                @Index(name = "idx_alert_user",    columnList = "user_id"),
                @Index(name = "idx_alert_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // what the booker searched for
    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(name = "source_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal sourceLat;

    @Column(name = "source_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal sourceLng;

    @Column(name = "destination_name", length = 255)
    private String destinationName;

    @Column(name = "destination_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLat;

    @Column(name = "destination_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLng;

    // time window the booker wants
    @Column(name = "window_from", nullable = false)
    private LocalDateTime windowFrom;

    @Column(name = "window_to", nullable = false)
    private LocalDateTime windowTo;

    // alert auto-expires at windowTo — scheduler cleans these up
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}