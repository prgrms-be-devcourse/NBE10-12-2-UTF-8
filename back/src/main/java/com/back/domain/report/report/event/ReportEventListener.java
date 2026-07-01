package com.back.domain.report.report.event;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.service.ChatMessageService;
import com.back.domain.report.report.entity.ReportedMessage;
import com.back.domain.report.report.repository.ReportedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportEventListener {
    private final ChatMessageService chatMessageService;
    private final ReportedMessageRepository reportedMessageRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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