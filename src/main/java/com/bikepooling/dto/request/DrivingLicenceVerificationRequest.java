package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class DrivingLicenceVerificationRequest {

    @NotBlank(message = "DL number is required")
    @Pattern(
            regexp = "[A-Z]{2}[0-9]{2}[0-9]{4}[0-9]{7}",
            message = "Invalid DL number format (e.g. MH1220230012345)"
    )
    private String dlNumber;

    @NotNull(message = "Date of birth is required for DL verification")
    private LocalDate dateOfBirth; // govt API requires DOB to verify DL
}