package com.bikepooling.entity;

import com.bikepooling.enums.KycMethod;
import com.bikepooling.enums.KycStatus;
import com.bikepooling.enums.KycType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_kyc_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DriverKycRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_type", nullable = false, length = 30)
    private KycType kycType;

    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    @Column(name = "document_image", length = 500)
    private String documentImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KycStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", length = 30)
    private KycMethod verificationMethod;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreationTimestamp
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
}
