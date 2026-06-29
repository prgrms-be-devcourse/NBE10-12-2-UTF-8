package com.back.domain.chat.chatRoomParticipant.service;

import com.back.domain.chat.chatRoomParticipant.repository.ChatRoomParticipantRepository;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomParticipantService {

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    public void validateAccess(UUID roomId, Member actor) {
        if (actor.isAdmin()) {
            return;
        }

        boolean isParticipant = chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(roomId, actor.getId());
        if (!isParticipant) {
            throw new ServiceException("403-1", "접근 권한이 없습니다.");
        }
    }
}