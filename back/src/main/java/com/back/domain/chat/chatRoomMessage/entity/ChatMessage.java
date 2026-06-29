package com.back.domain.chat.chatRoomMessage.entity;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ChatMessage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ChatRoomParticipant participant;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;


    public  ChatMessage(ChatRoom chatRoom, ChatRoomParticipant participant, String content) {
        this.chatRoom = chatRoom;
        this.participant = participant;
        this.content = content;
    }

}
