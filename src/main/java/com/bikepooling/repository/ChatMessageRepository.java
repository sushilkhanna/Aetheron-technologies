package com.bikepooling.repository;

import com.bikepooling.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT cm FROM ChatMessage cm
        JOIN FETCH cm.sender
        JOIN FETCH cm.receiver
        JOIN FETCH cm.template
        WHERE cm.template.id = :templateId
          AND ((cm.sender.id = :u1 AND cm.receiver.id = :u2)
            OR (cm.sender.id = :u2 AND cm.receiver.id = :u1))
        ORDER BY cm.createdAt DESC
        """)
    Page<ChatMessage> findConversation(
            @Param("templateId") Long templateId,
            @Param("u1") Long u1,
            @Param("u2") Long u2,
            Pageable pageable);

    @Query("""
        SELECT cm FROM ChatMessage cm
        JOIN FETCH cm.sender
        JOIN FETCH cm.receiver
        JOIN FETCH cm.template
        WHERE (cm.sender.id = :u1 AND cm.receiver.id = :u2)
           OR (cm.sender.id = :u2 AND cm.receiver.id = :u1)
        ORDER BY cm.createdAt DESC
        """)
    Page<ChatMessage> findUserConversation(
            @Param("u1") Long u1,
            @Param("u2") Long u2,
            Pageable pageable);

    @Query("""
        SELECT COUNT(cm) FROM ChatMessage cm
        WHERE cm.template.id = :templateId
          AND ((cm.sender.id = :u1 AND cm.receiver.id = :u2)
            OR (cm.sender.id = :u2 AND cm.receiver.id = :u1))
        """)
    long countConversationMessages(
            @Param("templateId") Long templateId,
            @Param("u1") Long u1,
            @Param("u2") Long u2);

    @Query("""
        SELECT COUNT(cm) FROM ChatMessage cm
        WHERE (cm.sender.id = :u1 AND cm.receiver.id = :u2)
           OR (cm.sender.id = :u2 AND cm.receiver.id = :u1)
        """)
    long countUserConversationMessages(
            @Param("u1") Long u1,
            @Param("u2") Long u2);

    @Query(value = """
        SELECT cm.id FROM chat_messages cm
        WHERE cm.template_id = :templateId
          AND ((cm.sender_id = :u1 AND cm.receiver_id = :u2)
            OR (cm.sender_id = :u2 AND cm.receiver_id = :u1))
        ORDER BY cm.created_at ASC
        LIMIT :deleteCount
        """, nativeQuery = true)
    List<Long> findOldestMessageIdsToPrune(
            @Param("templateId") Long templateId,
            @Param("u1") Long u1,
            @Param("u2") Long u2,
            @Param("deleteCount") int deleteCount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM ChatMessage cm WHERE cm.id IN :ids
        """)
    void deleteByIds(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ChatMessage cm
        SET cm.read = true
        WHERE cm.template.id = :templateId
          AND cm.sender.id = :senderId
          AND cm.receiver.id = :receiverId
          AND cm.read = false
        """)
    int markAsRead(
            @Param("templateId") Long templateId,
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ChatMessage cm
        SET cm.read = true
        WHERE cm.sender.id = :senderId
          AND cm.receiver.id = :receiverId
          AND cm.read = false
        """)
    int markUserAsRead(
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId);

    @Query("""
        SELECT COUNT(cm) FROM ChatMessage cm
        WHERE cm.receiver.id = :userId
          AND cm.read = false
        """)
    long countUnreadForUser(@Param("userId") Long userId);

    @Query("""
        SELECT cm FROM ChatMessage cm
        JOIN FETCH cm.sender
        JOIN FETCH cm.receiver
        JOIN FETCH cm.template
        WHERE cm.id IN (
            SELECT MAX(m.id) FROM ChatMessage m
            WHERE m.sender.id = :userId OR m.receiver.id = :userId
            GROUP BY m.template.id,
                     CASE WHEN m.sender.id = :userId THEN m.receiver.id ELSE m.sender.id END
        )
        ORDER BY cm.createdAt DESC
        """)
    List<ChatMessage> findRecentConversations(@Param("userId") Long userId);
}
