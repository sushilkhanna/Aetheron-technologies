package com.bikepooling.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String  token;
    private String  fullName;
    private String  phone;
    private String  role;
    private boolean phoneVerified;
}
