package com.bikepooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "coming_soon_users",
        indexes = {
                @Index(name = "idx_cs_user_phone", columnList = "phone", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComingSoonUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 20)
    private String platform; // ANDROID, IOS, WEB, APP

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean notified = false;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
