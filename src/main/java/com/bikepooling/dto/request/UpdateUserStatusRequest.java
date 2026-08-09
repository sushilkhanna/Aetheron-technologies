package com.bikepooling.dto.request;

import lombok.Getter;
import jakarta.validation.constraints.NotNull;

@Getter
public class UpdateUserStatusRequest {
    @NotNull(message = "active status is required")
    private Boolean active;
}