package com.back.domain.report.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReportRequestDto(
        @NotNull UUID roomId,
        @NotNull UUID reportedMessageId,
        @NotBlank @Size(max = 500) String reason
) {
}