package com.back.domain.match.matchRequest.event;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class TestMatchRequestEventHandler {
    private final MatchRequestService matchRequestService;
    private final StringRedisTemplate redisTemplate;

    @EventListener
    public void handleMatchRequestCreated(MatchRequestCreatedEvent event) {
        log.info("Synchronously triggering tryMatch in test environment for request: {}", event.getMatchRequestId());
        try {
            MatchRequest matchRequest = matchRequestService.findById(event.getMatchRequestId());
            redisTemplate.delete("match:queue:" + matchRequest.getIndustry().name());
            matchRequestService.tryMatch(matchRequest.getId());
        } catch (Exception e) {
            log.error("Failed to execute sync tryMatch in test environment for request: {}", event.getMatchRequestId(), e);
        }
    }
}
