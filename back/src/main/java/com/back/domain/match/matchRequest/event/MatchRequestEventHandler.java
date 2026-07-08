package com.back.domain.match.matchRequest.event;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MatchRequestEventHandler {
    private final MatchRequestService matchRequestService;
    private final StringRedisTemplate redisTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchRequestCreated(MatchRequestCreatedEvent event) {
        log.info("Triggering async tryMatch for match request: {}", event.getMatchRequestId());
        try {
            MatchRequest matchRequest = matchRequestService.findById(event.getMatchRequestId());

            String key = "match:queue:" + matchRequest.getIndustry().name();
            long score = java.sql.Timestamp.valueOf(matchRequest.getRequestedAt()).getTime();
            redisTemplate.opsForZSet().add(key, matchRequest.getId().toString(), score);

            matchRequestService.tryMatch(matchRequest.getId());
        } catch (Exception e) {
            log.error("Failed to execute async tryMatch for match request: {}", event.getMatchRequestId(), e);
        }
    }
}
