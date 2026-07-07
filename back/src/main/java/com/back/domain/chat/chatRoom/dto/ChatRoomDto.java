package com.back.domain.chat.chatRoom.dto;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRoomDto(
        UUID roomId,
        ChatRoomStatus status,
        int maxParticipants,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        boolean isBot
) {
    public ChatRoomDto(ChatRoom chatRoom, boolean isBot) {
        this(
                chatRoom.getId(),
                chatRoom.getStatus(),
                chatRoom.getMaxParticipants(),
                chatRoom.getCreatedAt(),
                chatRoom.getClosedAt(),
                isBot
        );
    }
}