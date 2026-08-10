package com.bikepooling.entity;

import com.bikepooling.enums.Gender;
import com.bikepooling.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_id",        columnList = "id", unique = true),
                @Index(name = "idx_user_phone",     columnList = "phone",    unique = true),
                @Index(name = "idx_user_google_id", columnList = "google_id", unique = true),
                @Index(name = "idx_user_email",     columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String phone;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(unique = true)
    private String email;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(name = "google_verified", nullable = false)
    @Builder.Default
    private boolean googleVerified = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    @Builder.Default
    private Gender gender = Gender.MALE;

    @Column(name = "address")
    private String address;

    @Column(name = "aadhaar_number", unique = true)
    private String aadhaarNumber;

    @Column(name = "aadhaar_verified", nullable = false)
    private boolean aadhaarVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "aadhaar_verification_method")
    private com.bikepooling.enums.KycMethod aadhaarVerificationMethod;

    @Column(name = "dl_number", unique = true)
    private String dlNumber;

    @Column(name = "dl_verified", nullable = false)
    private boolean dlVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "dl_verification_method")
    private com.bikepooling.enums.KycMethod dlVerificationMethod;

    // ── Emergency contacts (max 3) ──────────────────────────────────────────
    @Column(name = "emergency_contact_1_name")
    private String emergencyContact1Name;

    @Column(name = "emergency_contact_1_phone")
    private String emergencyContact1Phone;

    @Column(name = "emergency_contact_2_name")
    private String emergencyContact2Name;

    @Column(name = "emergency_contact_2_phone")
    private String emergencyContact2Phone;

    @Column(name = "emergency_contact_3_name")
    private String emergencyContact3Name;

    @Column(name = "emergency_contact_3_phone")
    private String emergencyContact3Phone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}