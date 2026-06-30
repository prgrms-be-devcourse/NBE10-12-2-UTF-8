package com.back.domain.dashboard.dashboard.dto;

import java.util.List;

public record DashboardResponseDto(
        MatchStatisticsDto matchStatistics,
        List<IndustryStatisticsDto> industryStatistics
) {}