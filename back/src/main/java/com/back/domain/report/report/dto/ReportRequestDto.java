package com.back.domain.report.report.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReportRequestDto(
        @NotNull UUID roomId,
        @NotNull UUID reportedMessageId,
        @NotNull String reason
) {
}