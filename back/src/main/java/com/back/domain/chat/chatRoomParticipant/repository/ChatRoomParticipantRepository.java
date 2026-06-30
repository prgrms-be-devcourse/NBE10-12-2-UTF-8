package com.back.domain.chat.chatRoomParticipant.repository;

import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, UUID> {
    boolean existsByChatRoomIdAndMemberId(UUID roomId, UUID memberId);

    Optional<ChatRoomParticipant> findByChatRoomIdAndMemberId(UUID roomId, UUID memberId);
}