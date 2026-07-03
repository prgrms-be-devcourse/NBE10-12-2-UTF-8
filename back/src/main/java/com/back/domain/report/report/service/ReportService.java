package com.back.domain.report.report.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.service.ChatRoomService;
import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.service.ChatMessageService;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import com.back.domain.chat.chatRoomParticipant.service.ChatRoomParticipantService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.event.ReportCreatedEvent;
import com.back.domain.report.report.repository.ReportRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ChatRoomParticipantService chatRoomParticipantService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Report createReport(Member reporter, UUID roomId, UUID reportedMessageId, String reason) {
        log.info("[ReportService] 신고 접수 시작 - Thread: {}", Thread.currentThread().getName());

        // 1. ChatRoomService를 통해 채팅방 검증 및 조회
        ChatRoom room = chatRoomService.getChatRoom(roomId);

        // 2. ChatMessageService를 통해 메시지 검증 및 조회
        ChatMessage targetMessage = chatMessageService.getMessage(reportedMessageId);

        // 3. 신고자가 해당 채팅방의 참여자(Participant)인지 서비스 위임 검증
        if (!chatRoomParticipantService.isParticipant(roomId, reporter.getId())) {
            throw new ServiceException("403-2", "채팅방 참여자만 신고할 수 있습니다.");
        }

        // 4. 신고 대상 메시지가 실제 요청한 채팅방(roomId)의 메시지가 맞는지 소유권 검증
        if (!targetMessage.getChatRoom().getId().equals(roomId)) {
            throw new ServiceException("400-3", "요청한 채팅방의 메시지가 아닙니다.");
        }

        // 5. 메시지 발송인(피신고자) 특정
        ChatRoomParticipant participant = targetMessage.getParticipant();
        Member reported = participant.getMember();

        // 6. 자기 자신 신고 금지 차단
        if (reporter.getId().equals(reported.getId())) {
            throw new ServiceException("400-1", "자신을 신고할 수 없습니다.");
        }

        // 7. 동일 메시지 중복 신고 차단
        if (reportRepository.existsByReporterAndReportedMessageId(reporter, reportedMessageId)) {
            throw new ServiceException("400-2", "이미 신고된 메시지입니다.");
        }

        // 8. 신고(Report) 객체 생성 및 저장
        Report report = reportRepository.save(new Report(reporter, reported, room, reportedMessageId, reason));

        // 9. 비동기 이벤트 발행 (엔티티 대신 식별자 ID만 전달하도록 수정)
        eventPublisher.publishEvent(new ReportCreatedEvent(report.getId(), room.getId(), reportedMessageId));

        log.info("[ReportService] 신고 접수 완료 - Thread: {}", Thread.currentThread().getName());
        return report;
    }

    // 관리자용 신고 목록 페이징 조회
    public Page<Report> getReportsForAdmin(Pageable pageable) {
        return reportRepository.findAllWithMember(pageable);
    }
}