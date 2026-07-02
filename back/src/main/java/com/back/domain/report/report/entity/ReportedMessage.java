package com.back.domain.report.report.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReportedMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;

    private UUID senderMemberId;   // 메시지 작성자 실제 회원 UUID
    private String senderNickname; // 메시지 작성자 닉네임

    @Column(columnDefinition = "TEXT")
    private String content;        // 대화 내용 복사

    private LocalDateTime sentAt;  // 원본 메시지 발송 시간 복사
    private boolean isTarget;      // 신고 유발 타겟 메시지 여부

    public ReportedMessage(Report report, UUID senderMemberId, String senderNickname, String content, LocalDateTime sentAt, boolean isTarget) {
        this.report = report;
        this.senderMemberId = senderMemberId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.sentAt = sentAt;
        this.isTarget = isTarget;
    }
}