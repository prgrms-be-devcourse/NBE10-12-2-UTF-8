package com.back.domain.match.matchRequest.entity;

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
public class MatchRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom room;
    private String situation;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private LocalDateTime requestedAt;

    public MatchRequest(Member member, String situation) {
        this.member = member;
        this.situation = situation;
        this.status = MatchStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public void matchWith(ChatRoom room) {
        this.room = room;
        this.status = MatchStatus.MATCHED;
    }

}
