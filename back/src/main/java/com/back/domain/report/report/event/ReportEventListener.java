package com.back.domain.report.report.event;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.service.ChatMessageService;
import com.back.domain.report.report.entity.ReportedMessage;
import com.back.domain.report.report.repository.ReportedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportEventListener {

    private final ChatMessageService chatMessageService; // 레포지토리 대신 서비스 참조
    private final ReportedMessageRepository reportedMessageRepository;

    @Async // 별도의 스레드에서 비동기 처리
    @EventListener // [일부러 일반 EventListener 사용해 트랜잭션 에러 유도]
    @Transactional
    public void handleReportCreatedEvent(ReportCreatedEvent event) {
        log.info("[ReportEventListener] 비동기 대화 백업 시작 - Thread: {}", Thread.currentThread().getName());

        List<ChatMessage> roomMessages = chatMessageService.getMessagesByRoom(event.room().getId());

        for (ChatMessage msg : roomMessages) {
            boolean isTarget = msg.getId().equals(event.targetMessageId());
            ReportedMessage reportedMsg = new ReportedMessage(
                    event.report(),
                    msg.getParticipant().getMember().getId(),
                    msg.getParticipant().getNickname(),
                    msg.getContent(),
                    msg.getCreatedAt(),
                    isTarget
            );
            reportedMessageRepository.save(reportedMsg);
        }

        log.info("[ReportEventListener] 비동기 대화 백업 완료 - Thread: {}", Thread.currentThread().getName());
    }
}