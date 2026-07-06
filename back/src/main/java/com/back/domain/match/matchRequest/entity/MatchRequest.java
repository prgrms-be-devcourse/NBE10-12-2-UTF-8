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

    // 비관적 락(@Lock)이 DB 엔진(H2 등)에 따라 기대만큼 안 먹힐 수 있어서,
    // 낙관적 락으로 이중 안전장치를 건다. 두 트랜잭션이 같은 row를 동시에 MATCHED로
    // 바꾸려고 하면, 나중에 커밋하는 쪽이 이 버전 충돌로 실패한다.
    //
    // 주의: claimPending()은 네이티브 벌크 UPDATE라 영속성 컨텍스트/dirty checking을
    // 거치지 않으므로 이 @Version 체크는 그 경로에서는 동작하지 않는다.
    // 실제 동시성 방어는 claimPending의 CAS(WHERE status='PENDING')가 전담하고,
    // 이 필드는 향후 일반 save() 경로로 MatchRequest를 수정하는 코드가 생길 때를 대비한 안전장치다.
    @Version
    private Long version =0L;
    
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
