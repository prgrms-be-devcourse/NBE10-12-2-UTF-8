package com.back.domain.match.matchRequest.dto;

import com.back.domain.match.matchRequest.entity.Situation;
import io.swagger.v3.oas.annotations.media.Schema;

public record SituationStatisticsDto(
        @Schema(description = "상황(situation) 종류")
        Situation situation,
        @Schema(description = "해당 상황으로 현재 대화 중인 인원 수")
        long count
) {
}
