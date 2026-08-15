package com.bikepooling.service;

import com.bikepooling.dto.request.ComingSoonRegisterRequest;
import com.bikepooling.dto.request.SendComingSoonSmsRequest;
import com.bikepooling.dto.response.AdminMessageResponse;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ComingSoonUserDTO;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.entity.ComingSoonUser;
import com.bikepooling.repository.ComingSoonUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComingSoonUserService {

    private static final Set<String> ALLOWED_SORT = Set.of("id", "phone", "platform", "notified", "createdAt");
    private final ComingSoonUserRepository comingSoonUserRepository;
    private final Msg91SmsClient msg91SmsClient;

    @Transactional
    public ApiResponse<ComingSoonUserDTO> registerPhone(ComingSoonRegisterRequest request, String ipAddress) {
        String normalizedPhone = normalizePhone(request.getPhone());

        // Check if phone already exists
        if (comingSoonUserRepository.existsByPhone(normalizedPhone)) {
            log.info("Phone number {} is already registered for coming soon access", maskPhone(normalizedPhone));
            ComingSoonUser existing = comingSoonUserRepository.findByPhone(normalizedPhone).orElse(null);
            ComingSoonUserDTO dto = existing != null ? toDTO(existing) : null;
            return ApiResponse.ok("This mobile number is already registered for early launch access!", dto);
        }

        String platform = (request.getPlatform() != null && !request.getPlatform().isBlank())
                ? request.getPlatform().toUpperCase()
                : "APP";

        ComingSoonUser user = ComingSoonUser.builder()
                .phone(normalizedPhone)
                .platform(platform)
                .ipAddress(ipAddress)
                .notified(false)
                .build();

        ComingSoonUser saved = comingSoonUserRepository.save(user);
        log.info("Successfully registered coming soon phone number: {}", maskPhone(normalizedPhone));

        return ApiResponse.ok("Registered successfully for early launch access!", toDTO(saved));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComingSoonUserDTO> getSubscribers(int page, int size, String search, Boolean notified, String sortBy, String sortDir) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSort = (sortBy != null && ALLOWED_SORT.contains(sortBy)) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(safeSort).ascending() : Sort.by(safeSort).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, sort);

        String searchPattern = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<ComingSoonUser> result = comingSoonUserRepository.searchSubscribers(searchPattern, notified, pageable);

        List<ComingSoonUserDTO> dtos = result.getContent().stream().map(this::toDTO).toList();

        return new PagedResponse<>(dtos, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }

    @Transactional
    public ApiResponse<AdminMessageResponse> sendSmsToSubscribers(SendComingSoonSmsRequest request) {
        List<ComingSoonUser> targetUsers = new ArrayList<>();

        if (Boolean.TRUE.equals(request.getTargetAll())) {
            String searchPattern = (request.getSearch() != null && !request.getSearch().trim().isEmpty())
                    ? request.getSearch().trim() : null;
            Pageable maxPageable = PageRequest.of(0, 10000, Sort.by("createdAt").descending());
            Page<ComingSoonUser> allMatching = comingSoonUserRepository.searchSubscribers(
                    searchPattern, request.getNotified(), maxPageable);
            targetUsers.addAll(allMatching.getContent());
        } else if (request.getSubscriberIds() != null && !request.getSubscriberIds().isEmpty()) {
            targetUsers.addAll(comingSoonUserRepository.findAllById(request.getSubscriberIds()));
        }

        if (targetUsers.isEmpty()) {
            return ApiResponse.ok("No subscribers selected or matched for SMS dispatch.",
                    AdminMessageResponse.builder().totalTargetUsers(0).sentSmsCount(0).build());
        }

        int sentSmsCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (ComingSoonUser user : targetUsers) {
            boolean sent = msg91SmsClient.sendSms(user.getPhone(), request.getMessage(), "COMING_SOON_NOTIFY");
            if (sent) {
                sentSmsCount++;
                user.setNotified(true);
                user.setNotifiedAt(now);
                comingSoonUserRepository.save(user);
            }
        }

        log.info("Sent coming soon SMS to {} out of {} targeted subscribers", sentSmsCount, targetUsers.size());

        AdminMessageResponse response = AdminMessageResponse.builder()
                .totalTargetUsers(targetUsers.size())
                .sentSmsCount(sentSmsCount)
                .build();

        return ApiResponse.ok("SMS notification dispatched successfully to " + sentSmsCount + " subscribers.", response);
    }

    public ComingSoonUserDTO toDTO(ComingSoonUser user) {
        return ComingSoonUserDTO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .platform(user.getPlatform())
                .createdAt(user.getCreatedAt())
                .notified(user.isNotified())
                .notifiedAt(user.getNotifiedAt())
                .build();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) return digits;
        if (digits.length() == 12 && digits.startsWith("91")) return digits.substring(2);
        return digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
