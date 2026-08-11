package com.bikepooling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMessageResponse {
    private int totalTargetUsers;
    private int sentPushCount;
    private int sentSmsCount;
    private int failedCount;
    private String statusMessage;
}
