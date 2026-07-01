package com.back.domain.match.matchRequest.entity;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.member.member.entity.Industry;
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
    // 한글 라벨로 DB에 저장해야 하는 값(사용자 노출용 코드성 데이터)은 AttributeConverter를 쓴다
    private Industry industry;
    private Situation situation;

    // 영문 name() 그대로 저장해도 무방한 내부 상태값은 @Enumerated(STRING)을 쓴다
    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private LocalDateTime requestedAt;


    public MatchRequest(Member member, Situation situation) {
        this.member = member;
        this.industry = member.getIndustry();
        this.situation = situation;
        this.status = MatchStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public void matchWith(ChatRoom room) {
        this.room = room;
        this.status = MatchStatus.MATCHED;
    }

}
