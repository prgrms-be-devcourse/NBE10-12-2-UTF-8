package com.back.domain.chat.chatRoomParticipant.entity;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoomParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private String nickname;

    private LocalDateTime joinedAt;

    public ChatRoomParticipant(ChatRoom chatRoom, Member member, String nickname) {
        this.chatRoom = chatRoom;
        this.member = member;
        this.nickname = nickname;
        this.joinedAt = LocalDateTime.now();
    }
}