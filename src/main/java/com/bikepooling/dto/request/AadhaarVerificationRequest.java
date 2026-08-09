package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AadhaarVerificationRequest {

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "\\d{12}", message = "Aadhaar number must be 12 digits")
    private String aadhaarNumber;
}
