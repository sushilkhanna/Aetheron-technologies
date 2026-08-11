package com.bikepooling.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendRideNotificationRequest {

    @NotEmpty(message = "At least one ride must be selected")
    @Size(max = 100, message = "Cannot send notifications to more than 100 rides per request")
    private List<@Valid RideSelection> rideSelections;

    /**
     * Target role for notification: "DRIVER", "BOOKER", or "BOTH"
     */
    @NotBlank(message = "Target role must be specified (DRIVER, BOOKER, or BOTH)")
    private String targetRole;

    private boolean sendPush = true;
    private boolean sendSms = false;

    @Size(max = 100, message = "Notification title cannot exceed 100 characters")
    private String title;

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RideSelection {
        @NotNull(message = "Ride instanceId is required")
        private Long instanceId;

        /**
         * "SCHEDULED" or "LIVE"
         */
        @NotBlank(message = "Ride type is required")
        private String rideType;
    }
}
