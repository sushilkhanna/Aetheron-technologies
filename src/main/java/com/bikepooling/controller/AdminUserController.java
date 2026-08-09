package com.bikepooling.controller;

import com.bikepooling.dto.request.UpdateUserRoleRequest;
import com.bikepooling.dto.request.UpdateUserStatusRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.PagedResponse;
import com.bikepooling.dto.response.UserDTO;
import com.bikepooling.enums.Role;
import com.bikepooling.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserDTO>>> getUsers(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(required = false)           String search,
            @RequestParam(required = false)           Boolean active,   // true / false / null (all)
            @RequestParam(required = false)           Role role,        // GUEST,USER,RIDER,DRIVER,ADMIN / null (all)
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {

        PagedResponse<UserDTO> data = adminUserService.getUsers(page, size, search, active, role, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", data));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> updateStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserStatusRequest request) {

        return ResponseEntity.ok(adminUserService.updateUserStatus(userId, request.getActive()));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> updateRole(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserRoleRequest request) {

        return ResponseEntity.ok(adminUserService.updateUserRole(userId, request.getRole()));
    }
}