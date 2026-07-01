package com.back.domain.match.matchRequest.dto;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.match.matchRequest.entity.MatchRequest;

import java.time.LocalDateTime;

public record MatchHistoryDto(
        LocalDateTime matchedAt,
        String industry,
        String situation,
        ChatRoomStatus status
) {
    public MatchHistoryDto(MatchRequest matchRequest) {
        this(
        matchRequest.getRoom().getCreatedAt(),
        matchRequest.getIndustry(),
        matchRequest.getSituation(),
        matchRequest.getRoom().getStatus()
        );
    }
}
