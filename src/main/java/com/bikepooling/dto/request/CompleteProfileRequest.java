package com.bikepooling.dto.request;

import com.bikepooling.enums.Gender;
import com.bikepooling.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteProfileRequest {

    @NotNull(message = "Gender is required")
    public Gender gender;

    @Email(message = "Invalid email format")
    public String email;

    @NotBlank(message = "Address is required")
    public String address;

    @NotNull(message = "Role is required (DRIVER or RIDER)")
    public Role role;  // Only DRIVER or RIDER allowed here
}