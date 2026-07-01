package com.back.domain.report.report.service;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.repository.ChatMessageRepository;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import com.back.domain.member.member.entity.Member;
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.entity.ReportedMessage;
import com.back.domain.report.report.repository.ReportRepository;
import com.back.domain.report.report.repository.ReportedMessageRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportedMessageRepository reportedMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public Report createReport(Member reporter, UUID roomId, UUID reportedMessageId, String reason) {
        log.info("[ReportService] 신고 접수 시작 - Thread: {}", Thread.currentThread().getName());

        // 1. 채팅방 및 대상 메시지 검증
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ServiceException("404-1", "채팅방을 찾을 수 없습니다."));

        ChatMessage targetMessage = chatMessageRepository.findById(reportedMessageId)
                .orElseThrow(() -> new ServiceException("404-2", "신고 대상 메시지를 찾을 수 없습니다."));

        // 2. 메시지 발송인(피신고자) 특정
        ChatRoomParticipant participant = targetMessage.getParticipant();
        Member reported = participant.getMember();

        // 3. 신고(Report) 객체 생성 및 저장
        Report report = reportRepository.save(new Report(reporter, reported, room, reportedMessageId, reason));

        // 4. [동기식 백업] 30개 대화 내용 즉시 복사 및 강제 1초 지연
        archiveMessagesSync(report, room, reportedMessageId);

        log.info("[ReportService] 신고 접수 완료 - Thread: {}", Thread.currentThread().getName());
        return report;
    }

    private void archiveMessagesSync(Report report, ChatRoom room, UUID targetMessageId) {
        log.info("[archiveMessagesSync] 대화 백업 시작 (동기) - Thread: {}", Thread.currentThread().getName());
        
        try {
            // 성능 지연을 시각적으로 체감하기 위한 강제 1초 슬립
            Thread.sleep(1000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 해당 방의 최근 메시지 가져오기 (실제 로직 구현 시 페이징 등으로 30개 제한)
        List<ChatMessage> roomMessages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(room.getId());

        for (ChatMessage msg : roomMessages) {
            boolean isTarget = msg.getId().equals(targetMessageId);
            ReportedMessage reportedMsg = new ReportedMessage(
                    report,
                    msg.getParticipant().getMember().getId(),
                    msg.getParticipant().getNickname(),
                    msg.getContent(),
                    msg.getCreatedAt(),
                    isTarget
            );
            reportedMessageRepository.save(reportedMsg);
        }
        
        log.info("[archiveMessagesSync] 대화 백업 완료 (동기) - Thread: {}", Thread.currentThread().getName());
    }
}