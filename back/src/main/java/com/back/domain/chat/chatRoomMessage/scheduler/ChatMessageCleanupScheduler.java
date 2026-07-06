package com.back.domain.chat.chatRoomMessage.scheduler;

import com.back.domain.chat.chatRoomMessage.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageCleanupScheduler {
    private final ChatMessageRepository chatMessageRepository;

    @Scheduled(cron = "${custom.scheduler.chat-clean.cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        int deletedCount = chatMessageRepository.deleteMessagesInRoomsClosedBefore(threshold);
        log.info("[메시지 휘발] 종료 후 24시간 경과 메시지 {}건 삭제 완료", deletedCount);
    }
}