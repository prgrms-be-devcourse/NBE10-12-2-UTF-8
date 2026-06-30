package com.back.domain.chat.chatRoomMessage.dto;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ChatRoomMessageResponseDto {
    private final UUID messageId;
    private final UUID roomId;
    private final String senderNickname;
    private final String content;
    private final LocalDateTime createdAt;

    public ChatRoomMessageResponseDto(ChatMessage message) {
        this.messageId = message.getId();
        this.roomId = message.getChatRoom().getId();
        this.senderNickname = message.getParticipant().getNickname();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}
