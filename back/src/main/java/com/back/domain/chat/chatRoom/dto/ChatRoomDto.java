package com.back.domain.chat.chatRoom.dto;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomDto(
        UUID roomId,
        ChatRoomStatus status,
        int maxParticipants,
        LocalDateTime createdAt
) {
    public ChatRoomDto(ChatRoom chatRoom) {
        this(
                chatRoom.getId(),
                chatRoom.getStatus(),
                chatRoom.getMaxParticipants(),
                chatRoom.getCreatedAt()
        );
    }
}