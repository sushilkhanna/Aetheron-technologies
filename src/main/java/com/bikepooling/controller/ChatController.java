package com.bikepooling.controller;

import com.bikepooling.config.UserPrincipal;
import com.bikepooling.dto.request.SendChatMessageRequest;
import com.bikepooling.dto.response.ApiResponse;
import com.bikepooling.dto.response.ChatConversationSummaryResponse;
import com.bikepooling.dto.response.ChatMessageResponse;
import com.bikepooling.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ── 1. Real-time WebSocket STOMP Send Handler & REST Fallback ────────────
    // WebSocket Destination: /app/chat.send
    @MessageMapping("/chat.send")
    public void handleStompMessage(@Payload @Valid SendChatMessageRequest req, Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated STOMP message attempt");
            return;
        }
        Long senderId = Long.parseLong(principal.getName());
        chatService.sendMessage(senderId, req);
    }

    /** REST Send Endpoint (Allowed ONLY if present ride is booked with recipient) */
    @PostMapping("/api/chat/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody SendChatMessageRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        ChatMessageResponse response = chatService.sendMessage(principal.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Message sent.", response));
    }

    // ── 2. Get Person List (Inbox Conversations List) ────────────────────────
    /** Retrieves list of persons with whom the user has chat messages and last message received */
    @GetMapping("/api/chat/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ChatConversationSummaryResponse>>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Conversations fetched.",
                chatService.getRecentConversations(principal.getUserId())));
    }

    // ── 3. See Messages Received from Particular Person ──────────────────────
    /** Retrieves chat message history by target User ID (e.g. GET /api/chat/messages/1) */
    @GetMapping("/api/chat/messages/{otherUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessagesByUser(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Page<ChatMessageResponse> messages = chatService.getConversationMessagesByUser(
                principal.getUserId(), otherUserId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Messages fetched.", messages));
    }

    /** Retrieves chat message history by Applicant ID (e.g. GET /api/chat/messages/applicant/1) */
    @GetMapping("/api/chat/messages/applicant/{applicantId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessagesByApplicant(
            @PathVariable Long applicantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Page<ChatMessageResponse> messages = chatService.getConversationMessagesByUser(
                principal.getUserId(), applicantId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Messages fetched.", messages));
    }

    /** Retrieves chat message history by Template ID and User ID (e.g. GET /api/chat/messages/3/1) */
    @GetMapping("/api/chat/messages/{templateId}/{otherUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessagesWithTemplate(
            @PathVariable Long templateId,
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Page<ChatMessageResponse> messages = chatService.getConversationMessages(
                principal.getUserId(), templateId, otherUserId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Messages fetched.", messages));
    }
}
