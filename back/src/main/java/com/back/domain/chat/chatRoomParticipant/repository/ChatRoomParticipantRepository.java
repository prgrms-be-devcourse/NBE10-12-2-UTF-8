package com.back.domain.chat.chatRoomParticipant.repository;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, UUID> {
    boolean existsByChatRoomIdAndMemberId(UUID roomId, UUID memberId);

    Optional<ChatRoomParticipant> findByChatRoomIdAndMemberId(UUID roomId, UUID memberId);

    // 참여자 조회 시 Member까지 fetch join - ChatMessageService.sendMessage에서
    // 참여자별 member.getId()/getEmail()을 순회 참조하므로 N+1 방지 필요
    @Query("""
           SELECT p FROM ChatRoomParticipant p
           JOIN FETCH p.member
           WHERE p.chatRoom.id = :chatRoomId
           """)
    List<ChatRoomParticipant> findByChatRoomId(@Param("chatRoomId") UUID chatRoomId);

    Optional<ChatRoomParticipant> findByMemberIdAndChatRoomStatus(UUID memberId, ChatRoomStatus status);
}