package com.bikepooling.dto.response;

import com.bikepooling.entity.DriverKycRequest;
import com.bikepooling.enums.KycMethod;
import com.bikepooling.enums.KycStatus;
import com.bikepooling.enums.KycType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverKycDTO {
    private Long id;
    private Long userId;
    private String driverName;
    private String email;
    private String phone;
    private KycType kycType;          // AADHAAR, DRIVING_LICENSE
    private String documentNumber;
    private String documentImage;
    private KycStatus status;         // PENDING_ADMIN, VERIFIED_BY_API, VERIFIED_BY_ADMIN, REJECTED
    private String statusLabel;       // Human-readable label
    private KycMethod verificationMethod; // API_KEY, ADMIN, NONE
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    public static DriverKycDTO from(DriverKycRequest kyc) {
        String label;
        if (kyc.getStatus() == KycStatus.VERIFIED_BY_API) {
            label = "Verified by API Key";
        } else if (kyc.getStatus() == KycStatus.VERIFIED_BY_ADMIN) {
            label = "Verified by Admin";
        } else if (kyc.getStatus() == KycStatus.PENDING_ADMIN) {
            label = "Pending Admin Verification";
        } else {
            label = "Rejected";
        }

        return DriverKycDTO.builder()
                .id(kyc.getId())
                .userId(kyc.getUser().getId())
                .driverName(kyc.getUser().getFullName())
                .email(kyc.getUser().getEmail())
                .phone(kyc.getUser().getPhone())
                .kycType(kyc.getKycType())
                .documentNumber(kyc.getDocumentNumber())
                .documentImage(kyc.getDocumentImage())
                .status(kyc.getStatus())
                .statusLabel(label)
                .verificationMethod(kyc.getVerificationMethod())
                .rejectionReason(kyc.getRejectionReason())
                .submittedAt(kyc.getSubmittedAt())
                .reviewedAt(kyc.getReviewedAt())
                .build();
    }
}
