package com.back.domain.match.matchRequest.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MatchRequestCreatedEvent {
    private final UUID matchRequestId;
}
