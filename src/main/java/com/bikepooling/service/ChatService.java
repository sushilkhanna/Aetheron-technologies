package com.bikepooling.service;

import com.bikepooling.dto.request.SendChatMessageRequest;
import com.bikepooling.dto.response.ChatConversationSummaryResponse;
import com.bikepooling.dto.response.ChatMessageResponse;
import com.bikepooling.entity.ChatMessage;
import com.bikepooling.entity.ScheduledRideApplication;
import com.bikepooling.entity.ScheduledRideTemplate;
import com.bikepooling.entity.User;
import com.bikepooling.exception.AppException;
import com.bikepooling.repository.ChatMessageRepository;
import com.bikepooling.repository.ScheduledRideApplicationDayRepository;
import com.bikepooling.repository.ScheduledRideApplicationRepository;
import com.bikepooling.repository.ScheduledRideTemplateRepository;
import com.bikepooling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository               chatRepo;
    private final ScheduledRideTemplateRepository      templateRepo;
    private final ScheduledRideApplicationRepository    appRepo;
    private final ScheduledRideApplicationDayRepository appDayRepo;
    private final UserRepository                      userRepo;
    private final SimpMessagingTemplate               messagingTemplate;
    private final FcmService                          fcmService;

    @Value("${chat.max-messages-per-conversation:50}")
    private int maxMessagesPerConversation;

    /**
     * Resolves target user from raw ID (which could be a direct User ID, applicant User ID, or Application ID).
     */
    public User resolveTargetUser(Long templateId, Long rawId) {
        if (rawId == null) {
            throw AppException.badRequest("Receiver user ID or application ID is required.");
        }

        // 1. First check if rawId matches a ScheduledRideApplication ID
        Optional<ScheduledRideApplication> appOpt = appRepo.findById(rawId);
        if (appOpt.isPresent()) {
            return appOpt.get().getBooker();
        }

        // 2. Otherwise find user directly by ID
        return userRepo.findById(rawId)
                .orElseThrow(() -> AppException.notFound("Receiver user or application not found for ID: " + rawId));
    }

    // ── Send Message ──────────────────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, SendChatMessageRequest req) {

        Long rawTargetId = req.getTargetRecipientId();
        User receiver = resolveTargetUser(req.getTemplateId(), rawTargetId);

        if (senderId.equals(receiver.getId())) {
            throw AppException.badRequest("You cannot send a message to yourself.");
        }

        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> AppException.notFound("Sender user not found"));

        // ── Present Ride Booking Check ─────────────────────────────────────────
        // Only allowed to send messages to a person with whom we have a PRESENT RIDE BOOKED / active application
        boolean hasPresentBooking = appDayRepo.hasPresentRideBooked(senderId, receiver.getId());
        long existingMsgs         = chatRepo.countUserConversationMessages(senderId, receiver.getId());

        if (!hasPresentBooking && existingMsgs == 0) {
            throw AppException.forbidden(
                    "You can only send messages to a user with whom you currently have a present ride booked or active application.");
        }

        // Locate ride template context
        ScheduledRideTemplate template = null;
        if (req.getTemplateId() != null) {
            template = templateRepo.findByIdWithDetails(req.getTemplateId()).orElse(null);
        }
        if (template == null) {
            var activeDays = appDayRepo.findActiveByBookerId(receiver.getId());
            for (var d : activeDays) {
                if (d.getInstance().getTemplate().getPostedBy().getId().equals(senderId)) {
                    template = d.getInstance().getTemplate();
                    break;
                }
            }
        }
        if (template == null) {
            var activeDaysSender = appDayRepo.findActiveByBookerId(senderId);
            for (var d : activeDaysSender) {
                if (d.getInstance().getTemplate().getPostedBy().getId().equals(receiver.getId())) {
                    template = d.getInstance().getTemplate();
                    break;
                }
            }
        }
        if (template == null) {
            List<ChatMessage> priorMsgs = chatRepo.findRecentConversations(senderId);
            for (ChatMessage m : priorMsgs) {
                if ((m.getSender().getId().equals(receiver.getId()) || m.getReceiver().getId().equals(receiver.getId())) && m.getTemplate() != null) {
                    template = m.getTemplate();
                    break;
                }
            }
        }

        if (template == null) {
            throw AppException.badRequest("Scheduled ride template context not found for messaging.");
        }

        // ── Save message ──────────────────────────────────────────────────────
        ChatMessage message = ChatMessage.builder()
                .template(template)
                .sender(sender)
                .receiver(receiver)
                .content(req.getContent().trim())
                .read(false)
                .build();

        message = chatRepo.save(message);

        // ── Enforce Stack / Pruning limit ─────────────────────────────────────
        pruneOldMessagesIfExceeded(template.getId(), senderId, receiver.getId());

        ChatMessageResponse response = ChatMessageResponse.from(message);

        // ── STOMP Real-Time Broadcast ─────────────────────────────────────────
        try {
            messagingTemplate.convertAndSend(
                    "/topic/user/" + receiver.getId() + "/messages", response);
            messagingTemplate.convertAndSend(
                    "/topic/user/" + sender.getId() + "/messages", response);

            log.debug("Chat message broadcasted via WebSocket STOMP: senderId={} receiverId={}",
                    senderId, receiver.getId());
        } catch (Exception e) {
            log.warn("Failed to broadcast chat message via WebSocket STOMP: {}", e.getMessage());
        }

        // ── FCM Push Notification ─────────────────────────────────────────────
        try {
            fcmService.sendToUser(
                    receiver.getId(),
                    "New message from " + sender.getFullName(),
                    message.getContent(),
                    java.util.Map.of(
                            "type", "CHAT_MESSAGE",
                            "templateId", String.valueOf(template.getId()),
                            "senderId", String.valueOf(senderId),
                            "senderName", sender.getFullName(),
                            "content", message.getContent()
                    )
            );
        } catch (Exception e) {
            log.warn("FCM chat notification skipped/failed: {}", e.getMessage());
        }

        return response;
    }

    // ── Stack Pruning Helper ─────────────────────────────────────────────────

    private void pruneOldMessagesIfExceeded(Long templateId, Long u1, Long u2) {
        long totalCount = chatRepo.countConversationMessages(templateId, u1, u2);
        if (totalCount > maxMessagesPerConversation) {
            int toDelete = (int) (totalCount - maxMessagesPerConversation);
            List<Long> idsToDelete = chatRepo.findOldestMessageIdsToPrune(templateId, u1, u2, toDelete);
            if (!idsToDelete.isEmpty()) {
                chatRepo.deleteByIds(idsToDelete);
                log.info("Pruned {} old chat messages (stack cap={}) for templateId={}, u1={}, u2={}",
                        idsToDelete.size(), maxMessagesPerConversation, templateId, u1, u2);
            }
        }
    }

    // ── Get Conversation Messages by User / Applicant ID (Auto Marks as Read) ──

    @Transactional
    public Page<ChatMessageResponse> getConversationMessagesByUser(
            Long currentUserId, Long otherUserId, int page, int size) {

        User otherUser = resolveTargetUser(null, otherUserId);
        Long resolvedOtherUserId = otherUser.getId();

        // Auto-mark incoming unread messages from this sender as read when reading!
        chatRepo.markUserAsRead(resolvedOtherUserId, currentUserId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(size > 0 ? size : 20, 50),
                Sort.by("createdAt").descending()
        );

        Page<ChatMessage> msgPage = chatRepo.findUserConversation(currentUserId, resolvedOtherUserId, pageable);
        return msgPage.map(ChatMessageResponse::from);
    }

    // Legacy method with templateId parameter (for backward compatibility)
    @Transactional
    public Page<ChatMessageResponse> getConversationMessages(
            Long currentUserId, Long templateId, Long otherUserId, int page, int size) {

        if (templateId == null) {
            return getConversationMessagesByUser(currentUserId, otherUserId, page, size);
        }

        User otherUser = resolveTargetUser(templateId, otherUserId);
        Long resolvedOtherUserId = otherUser.getId();

        // Auto-mark incoming unread messages as read
        chatRepo.markAsRead(templateId, resolvedOtherUserId, currentUserId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(size > 0 ? size : 20, 50),
                Sort.by("createdAt").descending()
        );

        Page<ChatMessage> msgPage = chatRepo.findConversation(templateId, currentUserId, resolvedOtherUserId, pageable);
        return msgPage.map(ChatMessageResponse::from);
    }

    // ── Mark as Read ─────────────────────────────────────────────────────────

    @Transactional
    public void markAsReadByUser(Long currentUserId, Long senderId) {
        User senderUser = resolveTargetUser(null, senderId);
        Long resolvedSenderId = senderUser.getId();

        int updated = chatRepo.markUserAsRead(resolvedSenderId, currentUserId);
        log.debug("Marked {} chat messages as read for senderId={}, reader={}",
                updated, resolvedSenderId, currentUserId);
    }

    @Transactional
    public void markAsRead(Long currentUserId, Long templateId, Long senderId) {
        if (templateId == null) {
            markAsReadByUser(currentUserId, senderId);
            return;
        }

        User senderUser = resolveTargetUser(templateId, senderId);
        Long resolvedSenderId = senderUser.getId();

        int updated = chatRepo.markAsRead(templateId, resolvedSenderId, currentUserId);
        log.debug("Marked {} chat messages as read for templateId={}, senderId={}, reader={}",
                updated, templateId, resolvedSenderId, currentUserId);
    }

    // ── List Recent Chat Conversations ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatConversationSummaryResponse> getRecentConversations(Long userId) {

        List<ChatMessage> lastMessages = chatRepo.findRecentConversations(userId);
        List<ChatConversationSummaryResponse> result = new ArrayList<>();

        for (ChatMessage msg : lastMessages) {
            User otherUser = msg.getSender().getId().equals(userId)
                    ? msg.getReceiver()
                    : msg.getSender();

            ScheduledRideTemplate template = msg.getTemplate();
            String routeSummary = template != null ? (template.getFromName() + " → " + template.getToName()) : "Bike Pooling Chat";

            boolean isFromMe = msg.getSender().getId().equals(userId);

            result.add(ChatConversationSummaryResponse.builder()
                    .templateId(template != null ? template.getId() : null)
                    .routeSummary(routeSummary)
                    .otherUserId(otherUser.getId())
                    .otherUserName(otherUser.getFullName())
                    .lastMessage(msg.getContent())
                    .lastMessageTime(msg.getCreatedAt())
                    .lastMessageRead(msg.isRead())
                    .lastMessageFromMe(isFromMe)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return chatRepo.countUnreadForUser(userId);
    }
}
