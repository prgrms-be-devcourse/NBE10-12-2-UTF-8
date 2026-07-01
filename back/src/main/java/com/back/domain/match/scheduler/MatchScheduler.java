package com.back.domain.match.scheduler;

import com.back.domain.match.matchRequest.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchScheduler {

    private final MatchRequestService matchRequestService;

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredMatchRequests() {
        matchRequestService.cancelExpiredRequests();
    }
}
