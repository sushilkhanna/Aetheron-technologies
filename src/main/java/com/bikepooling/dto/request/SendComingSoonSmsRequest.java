package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendComingSoonSmsRequest {

    /**
     * Selected subscriber IDs. Optional if targetAll is true.
     */
    private List<Long> subscriberIds;

    /**
     * If true, sends to all subscribers matching search/notified filters.
     */
    private Boolean targetAll;

    private String search;
    private Boolean notified;

    @NotBlank(message = "SMS message content is required")
    private String message;
}
