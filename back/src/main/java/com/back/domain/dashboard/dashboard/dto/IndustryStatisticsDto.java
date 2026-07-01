package com.back.domain.dashboard.dashboard.dto;

import com.back.domain.member.member.entity.Industry;

public record IndustryStatisticsDto(
        Industry industry,
        long count
) {}