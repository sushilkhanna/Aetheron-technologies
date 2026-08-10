package com.bikepooling.service;

import com.bikepooling.entity.DriverKycRequest;
import com.bikepooling.entity.User;
import com.bikepooling.enums.KycMethod;
import com.bikepooling.enums.KycStatus;
import com.bikepooling.enums.KycType;
import com.bikepooling.enums.Role;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.DriverKycRequestRepository;
import com.bikepooling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingLicenceVerificationService {

    private final UserRepository userRepository;
    private final DriverKycRequestRepository kycRequestRepository;

    @Value("${digilocker.api.key:#{null}}")
    private String digilockerApiKey;

    @Transactional
    public String verifyDrivingLicence(Long userId, String dlNumber, LocalDate dateOfBirth) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getRole() == Role.GUEST) {
            throw AppException.conflict("First complete your profile");
        }

        if (user.isDlVerified()) {
            throw AppException.conflict("Driving licence already verified for this user");
        }

        userRepository.findByDlNumber(dlNumber).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw AppException.conflict("DL number already linked to another account");
            }
        });

        user.setDlNumber(dlNumber);

        boolean apiKeyAvailable = (digilockerApiKey != null && !digilockerApiKey.isBlank() && !digilockerApiKey.equalsIgnoreCase("none"));

        DriverKycRequest kyc = kycRequestRepository.findByUserIdAndKycType(userId, KycType.DRIVING_LICENSE)
                .orElseGet(() -> DriverKycRequest.builder()
                        .user(user)
                        .kycType(KycType.DRIVING_LICENSE)
                        .build());

        kyc.setDocumentNumber(dlNumber);

        if (apiKeyAvailable) {
            log.info("Verifying Driving License via API Key for userId={}", userId);
            user.setDlVerified(true);
            user.setDlVerificationMethod(KycMethod.API_KEY);
            if (user.getRole() != Role.DRIVER) {
                user.setRole(Role.DRIVER);
            }

            kyc.setStatus(KycStatus.VERIFIED_BY_API);
            kyc.setVerificationMethod(KycMethod.API_KEY);
            kyc.setReviewedAt(LocalDateTime.now());
            userRepository.save(user);
            kycRequestRepository.save(kyc);
            return "VERIFIED_BY_API";
        } else {
            log.info("Govt DL API key unavailable. Sending DL number={} for userId={} to Admin Panel.", dlNumber, userId);
            user.setDlVerified(false);
            user.setDlVerificationMethod(KycMethod.NONE);

            kyc.setStatus(KycStatus.PENDING_ADMIN);
            kyc.setVerificationMethod(KycMethod.NONE);
            kyc.setRejectionReason(null);
            userRepository.save(user);
            kycRequestRepository.save(kyc);
            return "PENDING_ADMIN";
        }
    }
}