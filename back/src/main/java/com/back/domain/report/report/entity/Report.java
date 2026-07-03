package com.back.domain.report.report.entity;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.UUID;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Report extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member reporter; // 신고자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member reported; // 피신고자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom room;   // 발생한 채팅방

    // 신고 대상 원본 ChatMessage의 ID.
    // 원본 메시지는 24시간 후 Hard Delete 되므로 참조 무결성 예외 방지를 위해 외래키(FK) 없이 UUID만 보관함.
    private UUID reportedMessageId;

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