package com.back.domain.report.report.entity;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Report extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Member reporter; // 신고자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id")
    private Member reported; // 피신고자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom room;   // 발생한 채팅방

    private UUID reportedMessageId; // 신고 대상이 된 원본 메시지의 ID (UUID)

    @Column(columnDefinition = "TEXT")
    private String reason;   // 신고 사유

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    public Report(Member reporter, Member reported, ChatRoom room, UUID reportedMessageId, String reason) {
        this.reporter = reporter;
        this.reported = reported;
        this.room = room;
        this.reportedMessageId = reportedMessageId;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
    }
}