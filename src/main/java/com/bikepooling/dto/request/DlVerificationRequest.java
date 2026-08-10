package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlVerificationRequest {

    @NotBlank(message = "Driving License number is required")
    private String dlNumber;

    private String documentImage;
}
