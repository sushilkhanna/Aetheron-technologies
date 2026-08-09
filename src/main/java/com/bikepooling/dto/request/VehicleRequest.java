package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRequest {

    @NotBlank(message = "Vehicle number is required")
    @Pattern(
            regexp = "[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}",
            message = "Invalid vehicle number format (e.g. MH12AB1234)"
    )
    private String vehicleNumber;
}