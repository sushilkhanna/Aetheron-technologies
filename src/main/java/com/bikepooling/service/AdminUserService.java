package com.bikepooling.service;

import com.bikepooling.dto.request.SendAdminMessageRequest;
import com.bikepooling.dto.response.AdminMessageResponse;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.dto.response.UserDTO;
import com.bikepooling.entity.User;
import com.bikepooling.enums.Role;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.UserRepository;
import com.bikepooling.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<String> ALLOWED_SORT = Set.of("fullName", "email", "role", "active", "createdAt");
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final Msg91SmsClient msg91SmsClient;

    public PagedResponse<UserDTO> getUsers(int page, int size, String search,
                                           Boolean active, Role role,
                                           String sortBy, String sortDir) {

        String safeSort = ALLOWED_SORT.contains(sortBy) ? sortBy : "createdAt";
        int    safeSize = Math.min(Math.max(size, 1), 100);
        Sort   sort     = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(safeSort).ascending()
                : Sort.by(safeSort).descending();

        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, sort);
        Specification<User> spec = UserSpecification.withFilters(search, active, role);
        Page<User> result = userRepository.findAll(spec, pageable);

        List<UserDTO> dtos = result.getContent().stream().map(this::toDTO).toList();

        return new PagedResponse<>(dtos, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }

    private UserDTO toDTO(User u) {
        return UserDTO.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(maskPhone(u.getPhone()))
                .role(u.getRole())
                .gender(u.getGender())
                .active(u.isActive())
                .phoneVerified(u.isPhoneVerified())
                .aadhaarVerified(u.isAadhaarVerified())
                .dlVerified(u.isDlVerified())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        int maskLen = phone.length() - 5;
        return phone.substring(0, 3) + "X".repeat(maskLen) + phone.substring(phone.length() - 2);
    }

    @Transactional
    public ApiResponse<UserDTO> updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found with id: " + userId));


        if (user.getRole() == Role.ADMIN) {
            throw AppException.forbidden("Cannot change status of an ADMIN user");
        }

        user.setActive(active);
        userRepository.save(user);

        String msg = active ? "User activated successfully" : "User deactivated successfully";
        return ApiResponse.ok(msg, toDTO(user));
    }

    @Transactional
    public ApiResponse<UserDTO> updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found with id: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw AppException.forbidden("Cannot change role of an ADMIN user");
        }

        if (newRole == Role.ADMIN) {
            throw AppException.forbidden("Cannot assign ADMIN role from this endpoint");
        }

        user.setRole(newRole);
        userRepository.save(user);

        return ApiResponse.ok("User role updated to " + newRole, toDTO(user));
    }

    public ApiResponse<AdminMessageResponse> sendAdminMessage(SendAdminMessageRequest request) {
        List<User> targetUsers;

        if (Boolean.TRUE.equals(request.getTargetAllFiltered())) {
            Specification<User> spec = UserSpecification.withFilters(request.getSearch(), request.getActive(), request.getRole());
            targetUsers = userRepository.findAll(spec);
        } else if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            targetUsers = userRepository.findAllById(request.getUserIds());
        } else {
            throw AppException.badRequest("Please select at least one user or choose to target all filtered users");
        }

        if (targetUsers.isEmpty()) {
            throw AppException.badRequest("No matching users found to receive message");
        }

        int pushCount = 0;
        int smsCount = 0;
        int failedCount = 0;

        for (User u : targetUsers) {
            boolean success = false;

            // Push Notification via FCM
            if (request.isSendPush()) {
                try {
                    fcmService.sendToUser(
                            u.getId(),
                            request.getTitle(),
                            request.getMessage(),
                            Map.of("type", "ADMIN_ANNOUNCEMENT")
                    );
                    pushCount++;
                    success = true;
                } catch (Exception e) {
                    log.error("Failed to send FCM push to userId={}: {}", u.getId(), e.getMessage());
                }
            }

            // SMS via MSG91
            if (request.isSendSms()) {
                if (u.getPhone() == null || u.getPhone().isBlank()) {
                    log.warn("[ADMIN PANEL - SMS] Cannot send SMS to userId={} — phone number is empty", u.getId());
                } else {
                    try {
                        boolean smsOk = msg91SmsClient.sendSms(u.getPhone(), request.getMessage(), "ADMIN PANEL");
                        if (smsOk) {
                            smsCount++;
                            success = true;
                        }
                    } catch (Exception e) {
                        log.error("[ADMIN PANEL - SMS] Failed to send MSG91 SMS to userId={}: {}", u.getId(), e.getMessage());
                    }
                }
            }

            if (!success) {
                failedCount++;
            }
        }

        AdminMessageResponse response = AdminMessageResponse.builder()
                .totalTargetUsers(targetUsers.size())
                .sentPushCount(pushCount)
                .sentSmsCount(smsCount)
                .failedCount(failedCount)
                .statusMessage(String.format("Message processed for %d users (Push: %d, SMS: %d)", targetUsers.size(), pushCount, smsCount))
                .build();

        return ApiResponse.ok("Admin message broadcast completed successfully", response);
    }
}