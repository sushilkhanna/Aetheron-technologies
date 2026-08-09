package com.bikepooling.dto.response;

import com.bikepooling.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {

    private Long          id;
    private Long          templateId;
    private Long          senderId;
    private String        senderName;
    private Long          receiverId;
    private String        receiverName;
    private String        content;
    private boolean       read;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .templateId(msg.getTemplate() != null ? msg.getTemplate().getId() : null)
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getFullName())
                .receiverId(msg.getReceiver().getId())
                .receiverName(msg.getReceiver().getFullName())
                .content(msg.getContent())
                .read(msg.isRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
