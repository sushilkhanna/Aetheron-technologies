package com.bikepooling.dto.request;

import com.bikepooling.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    private Gender gender;

    @Email(message = "Invalid email format")
    private String email;

    private String address;
}
