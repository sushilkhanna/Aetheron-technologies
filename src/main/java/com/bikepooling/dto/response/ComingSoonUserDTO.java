package com.bikepooling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComingSoonUserDTO {
    private Long id;
    private String phone;
    private String platform;
    private LocalDateTime createdAt;
    private boolean notified;
    private LocalDateTime notifiedAt;
}
