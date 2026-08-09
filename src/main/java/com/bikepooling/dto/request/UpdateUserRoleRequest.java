package com.bikepooling.dto.request;

import com.bikepooling.enums.Role;
import lombok.Getter;
import jakarta.validation.constraints.NotNull;

@Getter
public class UpdateUserRoleRequest {
    @NotNull(message = "role is required")
    private Role role;
}