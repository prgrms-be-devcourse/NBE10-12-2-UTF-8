package com.back.domain.chat.chatRoom.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private ChatRoomStatus status;

    @Column(name = "max_participants")
    private int maxParticipants;

    private LocalDateTime closedAt;

    public ChatRoom(ChatRoomStatus status, int maxParticipants) {
        this.status = status;
        this.maxParticipants = maxParticipants;
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}