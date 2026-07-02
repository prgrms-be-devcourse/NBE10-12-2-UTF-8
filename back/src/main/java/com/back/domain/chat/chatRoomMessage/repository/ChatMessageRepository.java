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

    // 신고 메시지 작성일시(createdAt) 기준 그 이전의 대화들을 최근 순서(DESC)로 최대 30개만 제한 조회
    List<ChatMessage> findTop30ByChatRoomIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(UUID chatRoomId, LocalDateTime createdAt);

    @Query("""
           SELECT m FROM ChatMessage m
           JOIN FETCH m.participant p
           JOIN FETCH p.member
           WHERE m.chatRoom.id = :roomId
           ORDER BY m.createdAt ASC
           """)
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(@Param("roomId")UUID roomId);

    @Query("""
           SELECT m FROM ChatMessage m
           JOIN FETCH m.participant p
           JOIN FETCH p.member
           WHERE m.chatRoom.id = :roomId
           AND m.createdAt > :after
           ORDER BY m.createdAt ASC
           """)
    List<ChatMessage> findByChatRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
            @Param("roomId") UUID roomId,
            @Param("after") LocalDateTime after
    );
}