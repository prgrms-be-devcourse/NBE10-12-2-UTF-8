package com.back.domain.match.matchRequest.dto;

import com.back.domain.match.matchRequest.entity.Situation;

public record SituationStatisticsDto(
        Situation situation,
        long count
) {
}
