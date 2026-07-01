package com.back.domain.report.report.event;
import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.report.report.entity.Report;
import java.util.UUID;

public record ReportCreatedEvent(
        Report report,
        ChatRoom room,
        UUID targetMessageId
) {
}