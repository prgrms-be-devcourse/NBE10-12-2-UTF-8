package com.back.domain.chat.chatRoom.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.domain.chat.chatRoomParticipant.service.ChatRoomParticipantService;
import com.back.global.exception.ServiceException;
import com.back.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantService chatRoomParticipantService;
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatRoom getChatRoom(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "채팅방을 찾을 수 없습니다."));
    }

    @Transactional
    public ChatRoom createChatRoom(List<Member> members) {
        ChatRoom chatRoom = new ChatRoom(ChatRoomStatus.ACTIVE, members.size());
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        chatRoomParticipantService.createParticipants(savedRoom, members);
        return savedRoom;
    }

    @Transactional
    public ChatRoom closeChatRoom(UUID roomId, Member actor) {
        ChatRoom chatRoom = getChatRoom(roomId);

        chatRoomParticipantService.validateAccess(roomId, actor);

        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new ServiceException("409-1", "이미 종료된 채팅방입니다.");
        }

        chatRoom.close();

        // 채팅방 종료 시 Redis 메시지 캐시의 만료 시간(TTL)을 24시간으로 설정
        String key = "chat:room:" + roomId + ":messages";
        redisTemplate.expire(key, 24, TimeUnit.HOURS);

        return chatRoom;
    }

    public Optional<ChatRoom> findActiveChatRoom(Member actor) {
        return chatRoomParticipantService.findActiveChatRoomByMember(actor);
    }
}