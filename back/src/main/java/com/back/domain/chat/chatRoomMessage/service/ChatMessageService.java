package com.back.domain.chat.chatRoomMessage.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.domain.chat.chatRoomMessage.dto.ChatRoomMessageResponseDto;
import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.repository.ChatMessageRepository;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import com.back.domain.chat.chatRoomParticipant.repository.ChatRoomParticipantRepository;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatRoomMessageResponseDto sendMessage(UUID roomId, Member sender, String content) {
        // 메시지 내용 입력 여부 검증
        if (content == null || content.isBlank()) {
            throw new ServiceException("400-1", "메시지 내용을 입력해주세요.");
        }

        // 메시지 500자 초과 검증
        if (content.length() > 500) {
            throw new ServiceException("400-2", "메시지는 500자를 초과할 수 없습니다.");
        }

        // 채팅방 존재 여부 검증
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "채팅방을 찾을 수 없습니다."));

        // 종료된 채팅방에는 메시지 전송 불가
        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new ServiceException("409-1", "종료된 채팅방에는 메시지를 보낼 수 없습니다.");
        }

        // ChatMessage 생성에 필요한 참여자 정보 조회
        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndMemberId(roomId, sender.getId())
                .orElseThrow(() -> new ServiceException("403-1", "채팅방 참여자만 메시지를 보낼 수 있습니다."));

        ChatMessage message = chatMessageRepository.save(
                new ChatMessage(chatRoom, participant, content)
        );

        return new ChatRoomMessageResponseDto(message);
    }

    public ChatMessage getMessage(UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new com.back.global.exception.ServiceException("404-1", "신고 대상 메시지를 찾을 수 없습니다."));
    }
    public List<ChatMessage> getMessagesByRoom(UUID roomId) {
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId);
    }
}