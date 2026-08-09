package com.bikepooling.service;

import com.bikepooling.entity.User;
import com.bikepooling.enums.Role;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class DrivingLicenceVerificationService {

    private final UserRepository userRepository;

    public DrivingLicenceVerificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void verifyDrivingLicence(Long userId, String dlNumber, LocalDate dateOfBirth) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getRole() == Role.GUEST) {
            throw AppException.conflict("First complete your profile");
        }

        if (user.isDlVerified()) {
            throw AppException.conflict("Driving licence already verified for this user");
        }

        if (userRepository.existsByDlNumber(dlNumber)) {
            throw AppException.conflict("DL number already linked to another account");
        }

        // TODO: Replace mock with real govt API call
        //
        //   Example (Surepass / IDfy / Digio):
        //   DlVerifyResponse res = govtDlClient.verify(dlNumber, dateOfBirth);
        //   if (!res.isValid()) throw AppException.badRequest("DL verification failed: " + res.getReason());
        //
        boolean isVerifiedByGovt = mockGovtDlVerification(dlNumber, dateOfBirth);

        if (!isVerifiedByGovt) {
            throw AppException.badRequest("Driving licence verification failed");
        }

        user.setDlNumber(dlNumber);
        user.setDlVerified(true);
        user.setRole(Role.DRIVER);
        userRepository.save(user);
    }

    // MOCK: DL starting with "XX" = invalid, DOB in future = invalid (for testing)
    // Remove this entire method when real API is integrated
    private boolean mockGovtDlVerification(String dlNumber, LocalDate dateOfBirth) {
        if (dlNumber.startsWith("XX")) return false;
        if (dateOfBirth.isAfter(LocalDate.now())) return false;
        return true;
    }
}