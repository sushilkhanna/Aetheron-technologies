package com.bikepooling.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatConversationSummaryResponse {

    private Long          templateId;
    private String        routeSummary;
    private Long          otherUserId;
    private String        otherUserName;
    private String        lastMessage;
    private LocalDateTime lastMessageTime;
    private boolean       lastMessageRead;
    private boolean       lastMessageFromMe;
    private long          unreadCount;
}
