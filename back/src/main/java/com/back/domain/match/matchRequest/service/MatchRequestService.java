package com.back.domain.match.matchRequest.service;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchRequestService {
    private final MatchRequestRepository matchRequestRepository;

    public MatchRequest create(Member member, String situation) {
        if (matchRequestRepository.existsByMemberAndStatus(member, MatchStatus.PENDING)) {
            throw new ServiceException("409-1", "이미 진행 중인 매칭 요청이 있습니다.");
        }
        MatchRequest matchRequest = matchRequestRepository.save(new MatchRequest(member, situation));
        tryMatch(matchRequest);
        return matchRequest;
    }

    public void tryMatch(MatchRequest matchRequest) {
        String industry = matchRequest.getMember().getIndustry();
        String situation = matchRequest.getSituation();

        Optional<MatchRequest> opponent = matchRequestRepository
                .findPendingByIndustryAndSituation(industry, situation, MatchStatus.PENDING)
                .filter(r -> !r.equals(matchRequest));
        if (opponent.isEmpty()) {
            opponent = matchRequestRepository
                    .findPendingByIndustry(industry, MatchStatus.PENDING)
                    .filter(r -> !r.equals(matchRequest));
        }
        opponent.ifPresent(other -> {
            matchRequest.matchWith(null);
            other.matchWith(null);
            matchRequestRepository.save(matchRequest);
            matchRequestRepository.save(other);
        });
    }
}
