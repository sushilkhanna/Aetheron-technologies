package com.bikepooling.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private Long id;
    private String vehicleNumber;
    private boolean active;
    private LocalDateTime createdAt;
}