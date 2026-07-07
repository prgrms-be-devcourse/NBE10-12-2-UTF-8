package com.back.domain.match.matchRequest.dto;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.Situation;
import com.back.domain.member.member.entity.Industry;

import java.time.LocalDateTime;

public record MatchHistoryDto(
        LocalDateTime matchedAt,
        Industry industry,
        Situation situation,
        ChatRoomStatus status,
        boolean isBot
) {
    public MatchHistoryDto(MatchRequest matchRequest, boolean isBot) {
        this(
                matchRequest.getRoom().getCreatedAt(),
                matchRequest.getIndustry(),
                matchRequest.getSituation(),
                matchRequest.getRoom().getStatus(),
                isBot
        );
    }
}