package com.back.domain.report.report.event;

import java.util.UUID;

public record ReportCreatedEvent(
        UUID reportId,
        UUID roomId,
        UUID targetMessageId
) {
}