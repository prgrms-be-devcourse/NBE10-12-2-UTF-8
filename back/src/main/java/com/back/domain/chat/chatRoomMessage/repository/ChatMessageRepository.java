package com.back.domain.chat.chatRoomMessage.repository;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @Modifying
    @Query("""
            DELETE FROM ChatMessage m
            WHERE m.chatRoom.id IN (
                SELECT r.id FROM ChatRoom r
                WHERE r.status = com.back.domain.chat.chatRoom.entity.ChatRoomStatus.CLOSED
                AND r.closedAt <= :threshold
            )
            """)
    int deleteMessagesInRoomsClosedBefore(@Param("threshold") LocalDateTime threshold);

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(UUID id);
}