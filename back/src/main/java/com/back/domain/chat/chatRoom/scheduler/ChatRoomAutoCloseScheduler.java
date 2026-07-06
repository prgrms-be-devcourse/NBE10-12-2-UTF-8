package com.back.domain.chat.chatRoom.scheduler;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomAutoCloseScheduler {
    private final ChatRoomRepository chatRoomRepository;

    // 1분 간격으로 검사
    @Scheduled(cron = "${custom.scheduler.chat-close.cron:0 * * * * *}")
    @Transactional
    public void closeExpiredChatRooms() {
        // 기준 시간: 지금으로부터 10분 전
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        List<ChatRoom> expiredRooms = chatRoomRepository.findByStatusAndCreatedAtBefore(
                ChatRoomStatus.ACTIVE, threshold
        );

        for (ChatRoom room : expiredRooms) {
            room.close();
        }

        if (!expiredRooms.isEmpty()) {
            log.info("[채팅방 자동 종료] 10분 경과 활성 채팅방 {}건 자동 종료 완료", expiredRooms.size());
        }
    }
}