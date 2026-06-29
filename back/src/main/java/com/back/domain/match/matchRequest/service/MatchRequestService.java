package com.back.domain.match.matchRequest.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchRequestService {
    private final MatchRequestRepository matchRequestRepository;


}