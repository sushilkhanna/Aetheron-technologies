package com.bikepooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sos_location_pings", indexes = {
        @Index(name = "idx_sos_ping_alert", columnList = "sos_alert_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SosLocationPing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sos_alert_id", nullable = false)
    private SosAlert sosAlert;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @CreationTimestamp
    private LocalDateTime recordedAt;
}