package com.back.domain.match.matchRequest.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.service.ChatRoomService;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.entity.Situation;
import com.back.domain.match.matchRequest.entity.SituationSimilarity;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchRequestService {
    private final MatchRequestRepository matchRequestRepository;
    private final ChatRoomService chatRoomService;

    private static final long TIER1_THRESHOLD_SECONDS = 15; // 15초 후 유사 상황 매칭
    private static final long TIER2_THRESHOLD_SECONDS = 30; // 30초 후 산업군 전체 매칭

    private List<MatchRequest> findCandidates(Industry industry, Situation situation, long elapsedSeconds) {
        if(elapsedSeconds < TIER1_THRESHOLD_SECONDS) {
            return matchRequestRepository.findPendingByIndustryAndSituation(industry, situation, MatchStatus.PENDING);
        }
        if(elapsedSeconds <  TIER2_THRESHOLD_SECONDS) {
            Set<Situation> similarGroup = SituationSimilarity.getSimilarGroup(situation);
            return matchRequestRepository.findPendingByIndustryAndSituations(industry, similarGroup, MatchStatus.PENDING);
        }
        return matchRequestRepository.findPendingByIndustry(industry,  MatchStatus.PENDING);
    }

    private void connect(MatchRequest matchRequest, MatchRequest other) {
        ChatRoom chatRoom = chatRoomService.createChatRoom(List.of(matchRequest.getMember(), other.getMember()));
        matchRequest.matchWith(chatRoom);
        other.matchWith(chatRoom);

        matchRequestRepository.save(matchRequest);
        matchRequestRepository.save(other);
    }


    @Transactional
    public MatchRequest create(Member member, Situation situation) {
        if (member.getIndustry() == null) {
            throw new ServiceException("400-2", "산업군이 설정되지 않은 계정은 매칭을 요청할 수 없습니다.");
        }
        if (matchRequestRepository.existsByMemberAndStatus(member, MatchStatus.PENDING)) {
            throw new ServiceException("409-1", "이미 진행 중인 매칭 요청이 있습니다.");
        }
        MatchRequest matchRequest = matchRequestRepository.save(new MatchRequest(member, situation));
        tryMatch(matchRequest);
        return matchRequest;
    }

    @Transactional
    public void tryMatch(MatchRequest matchRequest) {
        if(matchRequest.getStatus() == MatchStatus.MATCHED) {
            return;
        }

        Industry industry = matchRequest.getMember().getIndustry();
        Situation situation = matchRequest.getSituation();
        long elapsedSeconds = Duration.between(matchRequest.getRequestedAt(), LocalDateTime.now()).getSeconds();

        List<MatchRequest> candidates = findCandidates(industry, situation, elapsedSeconds);
        Optional<MatchRequest> opponent = candidates.stream()
                .filter(r -> !r.equals(matchRequest))
                .findFirst();
        opponent.ifPresent(other -> connect(matchRequest, other));
    }

    @Transactional
    public void retryPendingMatches() {
        List<MatchRequest> pendingList = matchRequestRepository.findAllByStatus(MatchStatus.PENDING);
        for(MatchRequest request : pendingList) {
            tryMatch(request);
        }
    }

    public MatchRequest findById(UUID id) {
        return matchRequestRepository.findById(id)
                .orElseThrow(() -> new ServiceException("404-1", "매칭 요청을 찾을 수 없습니다."));
    }

    @Transactional
    public void cancel(MatchRequest matchRequest, Member actor) {
        if (!matchRequest.getMember().getId().equals(actor.getId())) {
            throw new ServiceException("403-1", "접근 권한이 없습니다.");
        }
        if (matchRequest.getStatus() == MatchStatus.MATCHED) {
            throw new ServiceException("409-1", "이미 매칭된 요청은 취소할 수 없습니다.");
        }
        matchRequestRepository.delete(matchRequest);
    }

    @Transactional
    public void cancelExpiredRequests() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(5);
        List<MatchRequest> expired = matchRequestRepository
                .findExpiredPending(MatchStatus.PENDING, expiredBefore);
        matchRequestRepository.deleteAll(expired);
    }

    public List<MatchRequest> findMatchHistoryByMember(Member member) {
        return matchRequestRepository.findByMemberAndRoomStatus(member, ChatRoomStatus.CLOSED);
    }



}
