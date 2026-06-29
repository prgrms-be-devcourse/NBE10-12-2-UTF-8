package com.back.domain.chat.chatRoomMessage.repository;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
}
