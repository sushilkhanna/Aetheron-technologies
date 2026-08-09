package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterFcmTokenRequest {

    @NotBlank(message = "FCM token is required")
    @Size(max = 512)
    private String token;

    // optional — helps identify the device for token rotation
    @Size(max = 255)
    private String deviceId;
}