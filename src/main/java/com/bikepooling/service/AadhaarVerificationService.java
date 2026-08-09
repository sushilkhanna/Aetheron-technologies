package com.bikepooling.service;

import com.bikepooling.entity.User;
import com.bikepooling.enums.Role;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AadhaarVerificationService {

    private final UserRepository userRepository;

    public AadhaarVerificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void verifyAadhaar(Long userId, String aadhaarNumber) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getRole() != Role.USER) {
            throw AppException.conflict("First complete your profile");
        }

        if (user.isAadhaarVerified()) {
            throw AppException.conflict("Aadhaar already verified for this user");
        }

        if (userRepository.existsByAadhaarNumber(aadhaarNumber)) {
            throw AppException.conflict("Aadhaar number already linked to another account");
        }

        // TODO: Replace mock with real govt API call
        //   boolean isValid = govtAadhaarClient.verify(aadhaarNumber);
        //   if (!isValid) throw AppException.badRequest("Aadhaar verification failed");
        boolean isVerifiedByGovt = mockGovtVerification(aadhaarNumber);

        if (!isVerifiedByGovt) {
            throw AppException.badRequest("Aadhaar verification failed");
        }

        user.setAadhaarNumber(aadhaarNumber);
        user.setAadhaarVerified(true);
        user.setRole(Role.RIDER);
        userRepository.save(user);
    }

    // MOCK: Aadhaar starting with 0 = invalid (for testing failures)
    // Remove this entire method when real API is integrated
    private boolean mockGovtVerification(String aadhaarNumber) {
        return !aadhaarNumber.startsWith("0");
    }
}