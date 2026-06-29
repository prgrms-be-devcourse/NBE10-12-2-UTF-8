package com.back.domain.match.matchRequest.dto;

import jakarta.validation.constraints.NotBlank;


public record MatchRequestDto(
        @NotBlank
        String situation
) { }
