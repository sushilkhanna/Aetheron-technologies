package com.bikepooling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendChatMessageRequest {

    @NotNull(message = "templateId is required")
    private Long templateId;

    /** Direct receiver User ID, applicant User ID, or Application ID. */
    private Long receiverId;

    /** Optional alias when frontend sends applicantId. */
    private Long applicantId;

    /** Optional alias when frontend sends applicationId. */
    private Long applicationId;

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;

    /** Returns whichever recipient identifier was supplied in request. */
    public Long getTargetRecipientId() {
        if (receiverId != null) return receiverId;
        if (applicantId != null) return applicantId;
        if (applicationId != null) return applicationId;
        return null;
    }
}
