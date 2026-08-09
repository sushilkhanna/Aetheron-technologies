package com.bikepooling.service;

import com.bikepooling.dto.request.CompleteProfileRequest;
import com.bikepooling.dto.request.UpdateProfileRequest;
import com.bikepooling.dto.response.ProfileResponse;
import com.bikepooling.entity.User;
import com.bikepooling.enums.Role;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional
    public ProfileResponse completeProfile(Long userId, CompleteProfileRequest req) {
        if (req.getRole() != Role.DRIVER && req.getRole() != Role.RIDER) {
            throw AppException.badRequest("Role must be either DRIVER or RIDER");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (isProfileComplete(user)) {
            throw AppException.conflict("Profile already completed. Use update endpoint.");
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            checkEmailConflict(req.getEmail(), userId);
            user.setEmail(req.getEmail());
        }

        user.setGender(req.getGender());
        user.setAddress(req.getAddress());
        user.setRole(req.getRole());

        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!isProfileComplete(user)) {
            throw AppException.badRequest("Please complete your profile first.");
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            checkEmailConflict(req.getEmail(), userId);
            user.setEmail(req.getEmail());
        }

        if (req.getGender() != null) {
            user.setGender(req.getGender());
        }

        if (req.getAddress() != null && !req.getAddress().isBlank()) {
            if (req.getAddress().trim().length() < 5 || req.getAddress().trim().length() > 255) {
                throw AppException.badRequest("Address must be between 5 and 255 characters");
            }
            user.setAddress(req.getAddress());
        }

        return toProfileResponse(userRepository.save(user));
    }

    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        return toProfileResponse(user);
    }

    private void checkEmailConflict(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> { throw AppException.conflict("Email already in use"); });
    }

    private boolean isProfileComplete(User user) {
        return user.getAddress() != null
                && user.getRole() != Role.USER;
    }

    private ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .address(user.getAddress())
                .gender(user.getGender())
                .role(user.getRole())
                .profileComplete(isProfileComplete(user))
                .build();
    }
}