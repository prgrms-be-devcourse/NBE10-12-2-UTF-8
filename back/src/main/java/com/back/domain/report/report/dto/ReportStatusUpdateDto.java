package com.back.domain.report.report.dto;

import com.back.domain.report.report.entity.ReportStatus;
import java.util.UUID;

public record ReportStatusUpdateDto(
        UUID reportId,
        ReportStatus status
) {
}