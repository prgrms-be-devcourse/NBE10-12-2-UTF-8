package com.back.domain.chat.chatRoomMessage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChatRoomMessageRequestDto {
    private String content;
}
