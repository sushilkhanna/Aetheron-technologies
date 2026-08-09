package com.bikepooling.dto.response;

import com.bikepooling.enums.Gender;
import com.bikepooling.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Gender gender;
    private Role role;
    private boolean profileComplete;
}