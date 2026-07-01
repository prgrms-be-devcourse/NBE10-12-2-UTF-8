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
    // 테스트 전용: 24시간 휘발 스케줄러 검증을 위해 과거 종료 시각을 주입
    public void closeAtForTest(LocalDateTime closedAt) {
        this.status = ChatRoomStatus.CLOSED;
        this.closedAt = closedAt;
    }
}