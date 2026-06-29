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

    private String industry;
    private String situation;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private LocalDateTime requested_at;
    private LocalDateTime modified_at;

    public MatchRequest(Member member, ChatRoom room, String industry, String situation) {
        this.member = member;
        this.room = room;
        this.industry = industry;
        this.situation = situation;
        this.status = MatchStatus.PENDING;
        this.requested_at = requested_at;
        this.modified_at = modified_at;

    }
}
