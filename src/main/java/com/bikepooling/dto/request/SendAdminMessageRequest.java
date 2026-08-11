package com.bikepooling.dto.request;

import com.bikepooling.enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SendAdminMessageRequest {

    /**
     * List of specific user IDs selected by admin.
     * Optional if targetAllFiltered is true.
     */
    private List<Long> userIds;

    /**
     * If true, sends to all users matching the search/active/role filters.
     */
    private Boolean targetAllFiltered;

    // Filter criteria when targetAllFiltered is true
    private String search;
    private Boolean active;
    private Role role;

    // Delivery channels
    private boolean sendPush = true;
    private boolean sendSms = false;

    // Message payload
    private String title = "Update from BikePooling";

    @NotBlank(message = "Message content is required")
    private String message;
}
