package com.bikepooling.service;

import com.bikepooling.dto.response.DriverKycDTO;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.entity.DriverKycRequest;
import com.bikepooling.entity.User;
import com.bikepooling.enums.*;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.DriverKycRequestRepository;
import com.bikepooling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKycService {

    private final DriverKycRequestRepository kycRepo;
    private final UserRepository userRepo;

    public PagedResponse<DriverKycDTO> getKycRequests(
            int page, int size,
            String statusStr, String typeStr, String search) {

        KycStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                status = KycStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (Exception e) {
                log.warn("Invalid KycStatus parameter: {}", statusStr);
            }
        }

        KycType type = null;
        if (typeStr != null && !typeStr.isBlank() && !"ALL".equalsIgnoreCase(typeStr.trim())) {
            try {
                type = KycType.valueOf(typeStr.trim().toUpperCase());
            } catch (Exception e) {
                log.warn("Invalid KycType parameter: {}", typeStr);
            }
        }

        String searchQuery = (search != null && !search.isBlank()) ? search.trim() : null;

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size > 0 ? size : 20, 100));
        Page<DriverKycRequest> pagedResult = kycRepo.searchKycRequests(status, type, searchQuery, pageable);

        Page<DriverKycDTO> dtoPage = pagedResult.map(DriverKycDTO::from);
        return PagedResponse.of(dtoPage);
    }

    public Map<String, Object> getKycStats() {
        long pendingCount = kycRepo.countByStatus(KycStatus.PENDING_ADMIN);
        long aadhaarPending = kycRepo.countByKycTypeAndStatus(KycType.AADHAAR, KycStatus.PENDING_ADMIN);
        long dlPending = kycRepo.countByKycTypeAndStatus(KycType.DRIVING_LICENSE, KycStatus.PENDING_ADMIN);
        long verifiedApi = kycRepo.countByStatus(KycStatus.VERIFIED_BY_API);
        long verifiedAdmin = kycRepo.countByStatus(KycStatus.VERIFIED_BY_ADMIN);
        long rejectedCount = kycRepo.countByStatus(KycStatus.REJECTED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", pendingCount);
        stats.put("aadhaarPending", aadhaarPending);
        stats.put("dlPending", dlPending);
        stats.put("totalVerified", verifiedApi + verifiedAdmin);
        stats.put("verifiedApi", verifiedApi);
        stats.put("verifiedAdmin", verifiedAdmin);
        stats.put("rejectedCount", rejectedCount);
        return stats;
    }

    @Transactional
    public DriverKycDTO approveKyc(Long kycId, Long adminId) {
        DriverKycRequest kyc = kycRepo.findById(kycId)
                .orElseThrow(() -> AppException.notFound("KYC request not found"));

        User admin = adminId != null ? userRepo.findById(adminId).orElse(null) : null;
        User user = kyc.getUser();

        kyc.setStatus(KycStatus.VERIFIED_BY_ADMIN);
        kyc.setVerificationMethod(KycMethod.ADMIN);
        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setReviewedBy(admin);
        kyc.setRejectionReason(null);

        if (kyc.getKycType() == KycType.AADHAAR) {
            user.setAadhaarNumber(kyc.getDocumentNumber());
            user.setAadhaarVerified(true);
            user.setAadhaarVerificationMethod(KycMethod.ADMIN);
            if (user.getRole() == Role.USER) {
                user.setRole(Role.RIDER);
            }
        } else if (kyc.getKycType() == KycType.DRIVING_LICENSE) {
            user.setDlNumber(kyc.getDocumentNumber());
            user.setDlVerified(true);
            user.setDlVerificationMethod(KycMethod.ADMIN);
            if (user.getRole() != Role.DRIVER) {
                user.setRole(Role.DRIVER);
            }
        }

        userRepo.save(user);
        kyc = kycRepo.save(kyc);
        log.info("KYC request id={} APPROVED by adminId={}", kycId, adminId);
        return DriverKycDTO.from(kyc);
    }

    @Transactional
    public DriverKycDTO rejectKyc(Long kycId, Long adminId, String reason) {
        DriverKycRequest kyc = kycRepo.findById(kycId)
                .orElseThrow(() -> AppException.notFound("KYC request not found"));

        User admin = adminId != null ? userRepo.findById(adminId).orElse(null) : null;
        User user = kyc.getUser();

        kyc.setStatus(KycStatus.REJECTED);
        kyc.setVerificationMethod(KycMethod.ADMIN);
        kyc.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Rejected by Admin");
        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setReviewedBy(admin);

        if (kyc.getKycType() == KycType.AADHAAR) {
            user.setAadhaarVerified(false);
        } else if (kyc.getKycType() == KycType.DRIVING_LICENSE) {
            user.setDlVerified(false);
        }

        userRepo.save(user);
        kyc = kycRepo.save(kyc);
        log.info("KYC request id={} REJECTED by adminId={}. Reason: {}", kycId, adminId, reason);
        return DriverKycDTO.from(kyc);
    }
}
