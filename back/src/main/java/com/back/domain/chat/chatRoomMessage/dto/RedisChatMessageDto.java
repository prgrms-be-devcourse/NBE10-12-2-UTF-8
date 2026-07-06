package com.back.domain.chat.chatRoomMessage.dto;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedisChatMessageDto implements Serializable {
    private UUID messageId;
    private UUID roomId;
    private String senderNickname;
    private UUID senderMemberId;
    private String content;
    private LocalDateTime createdAt;

    public RedisChatMessageDto(ChatMessage message) {
        this.messageId = message.getId();
        this.roomId = message.getChatRoom().getId();
        this.senderNickname = message.getParticipant().getNickname();
        this.senderMemberId = message.getParticipant().getMember().getId();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}
