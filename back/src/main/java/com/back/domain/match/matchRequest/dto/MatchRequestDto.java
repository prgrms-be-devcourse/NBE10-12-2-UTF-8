package com.back.domain.match.matchRequest.dto;

import com.back.domain.match.matchRequest.entity.Situation;
import jakarta.validation.constraints.NotNull;


public record MatchRequestDto(
        @NotNull(message = "상황을 선택해주세요.")
        Situation situation
) { }
