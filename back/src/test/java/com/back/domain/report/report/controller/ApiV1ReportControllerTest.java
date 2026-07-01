package com.back.domain.report.report.controller;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.repository.ChatMessageRepository;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import com.back.domain.chat.chatRoomParticipant.repository.ChatRoomParticipantRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private com.back.domain.report.report.repository.ReportRepository reportRepository;

    @Autowired
    private com.back.domain.report.report.repository.ReportedMessageRepository reportedMessageRepository;

    @Test
    @DisplayName("신고 접수 성공")
    void t1() throws Exception {
        // Given
        // 1. 신고자와 피신고자 회원가입 및 토큰 발급
        Member reporter = memberService.join("reporter@test.com", "1234", "IT", "USER");
        Member reported = memberService.join("reported@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(reporter);

        // 2. 대화방 생성 및 참여자 등록
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        ChatRoomParticipant p1 = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, reporter, "익명의 동료"));
        ChatRoomParticipant p2 = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, reported, "익명의 동료"));

        // 3. 원본 욕설 메시지 저장
        ChatMessage targetMessage = chatMessageRepository.save(new ChatMessage(chatRoom, p2, "너 일 그따구로 할 거면 사표 써라"));
        UUID reportedMessageId = targetMessage.getId();

        // Given 단계 데이터 영속성 적재를 위한 강제 커밋
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // API 격리 실행을 위한 새로운 트랜잭션 수동 개시
        TestTransaction.start();

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/reports")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "roomId": "%s",
                                            "reportedMessageId": "%s",
                                            "reason": "사내 비방 및 욕설"
                                        }
                                        """.formatted(roomId, reportedMessageId))
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("신고가 접수되었습니다."));

        // API가 마친 메인 트랜잭션 강제 커밋 -> AFTER_COMMIT 이벤트 유발
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // 비동기 스레드가 돌 때까지 0.5초 대기
        Thread.sleep(500);

        // 최종 비동기 메시지 복사 개수 검증
        org.assertj.core.api.Assertions.assertThat(reportedMessageRepository.count()).isEqualTo(1);

        // 수동 클린업 (수동 커밋을 수행했으므로 강제 롤백을 못해 외래키 종속성 역순으로 직접 제거)
        reportedMessageRepository.deleteAll();
        reportRepository.deleteAll(); // ReportRepository 주입 추가 및 삭제 처리
        chatMessageRepository.deleteAll();
        chatRoomParticipantRepository.deleteAll();
        chatRoomRepository.deleteAll();
    }
}