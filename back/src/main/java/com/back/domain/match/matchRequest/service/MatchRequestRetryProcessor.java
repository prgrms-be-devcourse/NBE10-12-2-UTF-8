package com.back.domain.match.matchRequest.service;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// MatchRequestRetryProcessor.java
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchRequestRetryProcessor {
    private final MatchRequestRepository matchRequestRepository;
    private final ApplicationContext applicationContext;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryOne(UUID matchRequestId) {
        MatchRequest matchRequest = matchRequestRepository.findByIdWithMember(matchRequestId)
                .orElseThrow(() -> new ServiceException("404-1", "매칭 요청을 찾을 수 없습니다."));
        applicationContext.getBean(MatchRequestService.class).tryMatch(matchRequest);
    }
}