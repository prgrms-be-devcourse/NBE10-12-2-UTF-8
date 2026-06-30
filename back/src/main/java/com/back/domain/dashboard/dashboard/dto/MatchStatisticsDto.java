package com.back.domain.dashboard.dashboard.dto;

public record MatchStatisticsDto(
        long totalMembers,
        long todayMatches,
        long activeChatRooms
) {}
