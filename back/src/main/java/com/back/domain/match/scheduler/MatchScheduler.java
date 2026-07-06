package com.back.domain.match.scheduler;

import com.back.domain.match.matchRequest.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchScheduler {

    private final MatchRequestService matchRequestService;

    @Scheduled(fixedDelayString = "${custom.scheduler.match.retry-delay:10000}", initialDelayString = "${custom.scheduler.match.initial-delay:0}")
    public void retryPendingMatches() {
        matchRequestService.retryPendingMatches();
    }

    @Scheduled(fixedDelayString = "${custom.scheduler.match.cancel-delay:60000}", initialDelayString = "${custom.scheduler.match.initial-delay:0}")
    public void cancelExpiredMatchRequests() {
        matchRequestService.cancelExpiredRequests();
    }

}
