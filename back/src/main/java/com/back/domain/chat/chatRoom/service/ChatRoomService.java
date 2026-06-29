package com.back.domain.chat.chatRoom.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoom getChatRoom(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "채팅방을 찾을 수 없습니다."));
    }
}