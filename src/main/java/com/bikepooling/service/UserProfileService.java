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
import com.bikepooling.dto.request.UpdateEmergencyContactsRequest;

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
        user.setRole(Role.USER);

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

    @Transactional
    public void updateEmergencyContacts(Long userId, UpdateEmergencyContactsRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        validateContact(req.getContact1Name(), req.getContact1Phone(), "1");
        validateContact(req.getContact2Name(), req.getContact2Phone(), "2");
        validateContact(req.getContact3Name(), req.getContact3Phone(), "3");

        user.setEmergencyContact1Name(blankToNull(req.getContact1Name()));
        user.setEmergencyContact1Phone(blankToNull(req.getContact1Phone()));
        user.setEmergencyContact2Name(blankToNull(req.getContact2Name()));
        user.setEmergencyContact2Phone(blankToNull(req.getContact2Phone()));
        user.setEmergencyContact3Name(blankToNull(req.getContact3Name()));
        user.setEmergencyContact3Phone(blankToNull(req.getContact3Phone()));

        userRepository.save(user);
    }

    private void validateContact(String name, String phone, String slot) {
        boolean nameBlank = name == null || name.isBlank();
        boolean phoneBlank = phone == null || phone.isBlank();
        if (nameBlank != phoneBlank) {
            throw AppException.badRequest("Emergency contact " + slot + " needs both name and phone, or leave both empty");
        }
        if (!phoneBlank && !phone.replaceAll("[^0-9]", "").matches("^[0-9]{10,12}$")) {
            throw AppException.badRequest("Emergency contact " + slot + " phone number looks invalid");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
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
                && user.getRole() != null
                && user.getRole() != Role.GUEST;
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