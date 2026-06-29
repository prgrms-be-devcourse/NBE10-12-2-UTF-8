package com.back.domain.match.matchRequest.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class MatchRequest extends BaseEntity {

    @Id
    private UUID matchRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String roomId;
    private String industry;
    private String situation;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private LocalDateTime requestedAt;

    public MatchRequest(Member member, String roomId, String industry, String situation) {
        this.matchRequestId = UUID.randomUUID();
        this.member = member;
        this.roomId = roomId;
        this.industry = industry;
        this.situation = situation;
        this.status = MatchStatus.PENDING;
        this.requestedAt = requestedAt;
    }
}
