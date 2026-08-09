package com.bikepooling.dto.response;

import com.bikepooling.enums.Role;
import com.bikepooling.enums.Gender;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class UserDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Gender gender;
    private boolean active;
    private boolean phoneVerified;
    private boolean aadhaarVerified;
    private boolean dlVerified;
    private LocalDateTime createdAt;
}