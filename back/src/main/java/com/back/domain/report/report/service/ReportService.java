package com.back.domain.report.report.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.service.ChatRoomService;
import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.service.ChatMessageService;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import com.back.domain.member.member.entity.Member;
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.event.ReportCreatedEvent;
import com.back.domain.report.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Report createReport(Member reporter, UUID roomId, UUID reportedMessageId, String reason) {
        log.info("[ReportService] 신고 접수 시작 - Thread: {}", Thread.currentThread().getName());

        // 1. ChatRoomService를 통해 채팅방 검증 및 조회
        ChatRoom room = chatRoomService.getChatRoom(roomId);

        // 2. ChatMessageService를 통해 메시지 검증 및 조회
        ChatMessage targetMessage = chatMessageService.getMessage(reportedMessageId);

        // 3. 메시지 발송인(피신고자) 특정
        ChatRoomParticipant participant = targetMessage.getParticipant();
        Member reported = participant.getMember();

        // 4. 신고(Report) 객체 생성 및 저장
        Report report = reportRepository.save(new Report(reporter, reported, room, reportedMessageId, reason));

        // 5. 비동기 이벤트 발행
        eventPublisher.publishEvent(new ReportCreatedEvent(report.getId(), room.getId(), reportedMessageId));

        log.info("[ReportService] 신고 접수 완료 - Thread: {}", Thread.currentThread().getName());
        return report;
    }
}