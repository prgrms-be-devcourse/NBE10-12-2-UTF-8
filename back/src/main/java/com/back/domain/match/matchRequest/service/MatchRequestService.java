package com.back.domain.match.matchRequest.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.service.ChatRoomService;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchRequestService {
    private final MatchRequestRepository matchRequestRepository;
    private final ChatRoomService chatRoomService;

    @Transactional
    public MatchRequest create(Member member, String situation) {
        if (matchRequestRepository.existsByMemberAndStatus(member, MatchStatus.PENDING)) {
            throw new ServiceException("409-1", "이미 진행 중인 매칭 요청이 있습니다.");
        }
        MatchRequest matchRequest = matchRequestRepository.save(new MatchRequest(member, situation));
        tryMatch(matchRequest);
        return matchRequest;
    }

    @Transactional
    public void tryMatch(MatchRequest matchRequest) {
        String industry = matchRequest.getMember().getIndustry();
        String situation = matchRequest.getSituation();

        Optional<MatchRequest> opponent = matchRequestRepository
                .findPendingByIndustryAndSituation(industry, situation, MatchStatus.PENDING)
                .stream()
                .filter(r -> !r.equals(matchRequest))
                .findFirst();
        if (opponent.isEmpty()) {
            opponent = matchRequestRepository
                    .findPendingByIndustry(industry, MatchStatus.PENDING)
                    .stream()
                    .filter(r -> !r.equals(matchRequest))
                    .findFirst();
        }
        opponent.ifPresent(other -> {
            ChatRoom chatRoom = chatRoomService.createChatRoom(List.of(matchRequest.getMember(), other.getMember()));

            matchRequest.matchWith(chatRoom);
            other.matchWith(chatRoom);

            matchRequestRepository.save(matchRequest);
            matchRequestRepository.save(other);
        });
    }



    public MatchRequest findById(UUID id) {
        return matchRequestRepository.findById(id)
                .orElseThrow(() -> new ServiceException("404-1", "매칭 요청을 찾을 수 없습니다."));
    }





}
