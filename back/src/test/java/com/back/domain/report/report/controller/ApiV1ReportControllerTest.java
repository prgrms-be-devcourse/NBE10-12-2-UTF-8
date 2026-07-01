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
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.repository.ReportRepository;
import com.back.domain.report.report.repository.ReportedMessageRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.back.domain.member.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1ReportControllerTest {
    private final List<Member> createdMembers = new ArrayList<>();

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
    private ReportRepository reportRepository;

    @Autowired
    private ReportedMessageRepository reportedMessageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        if (TestTransaction.isActive()) {
            TestTransaction.end();
        }
        TestTransaction.start();
        reportedMessageRepository.deleteAll();
        reportRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomParticipantRepository.deleteAll();
        chatRoomRepository.deleteAll();
        createdMembers.forEach(memberRepository::delete);
        createdMembers.clear();
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    @Test
    @DisplayName("신고 접수 성공")
    void t1() throws Exception {
        // Given
        // 1. 신고자와 피신고자 회원가입 및 토큰 발급
        Member reporter = memberService.join("reporter@test.com", "1234", "IT", "USER");
        Member reported = memberService.join("reported@test.com", "1234", "IT", "USER");
        createdMembers.add(reporter);
        createdMembers.add(reported);
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
                .andExpect(jsonPath("$.msg").value("신고 생성 성공"));

        // API가 마친 메인 트랜잭션 강제 커밋 -> AFTER_COMMIT 이벤트 유발
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Awaitility를 사용해 비동기 저장이 완료되는 순간 즉시 대기 탈출
        await()
                .atMost(2, SECONDS)
                .untilAsserted(() ->
                        org.assertj.core.api.Assertions.assertThat(reportedMessageRepository.count()).isEqualTo(1)
                );
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 ID로 신고 시 실패")
    void t2() throws Exception {
        // Given
        Member reporter = memberService.join("reporter2@test.com", "1234", "IT", "USER");
        createdMembers.add(reporter);
        String accessToken = memberService.genAccessToken(reporter);
        UUID nonExistentRoomId = UUID.randomUUID();
        UUID mockMessageId = UUID.randomUUID();

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
                                        """.formatted(nonExistentRoomId, mockMessageId))
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 메시지 ID로 신고 시 실패")
    void t3() throws Exception {
        // Given
        Member reporter = memberService.join("reporter3@test.com", "1234", "IT", "USER");
        createdMembers.add(reporter);
        String accessToken = memberService.genAccessToken(reporter);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        UUID nonExistentMessageId = UUID.randomUUID();

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
                                        """.formatted(roomId, nonExistentMessageId))
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"))
                .andExpect(jsonPath("$.msg").value("신고 대상 메시지를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("비로그인 사용자가 신고 시도 시 실패")
    void t4() throws Exception {
        // Given
        UUID mockRoomId = UUID.randomUUID();
        UUID mockMessageId = UUID.randomUUID();

        // When (액세스 토큰 쿠키 없이 호출)
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "roomId": "%s",
                                            "reportedMessageId": "%s",
                                            "reason": "사내 비방 및 욕설"
                                        }
                                        """.formatted(mockRoomId, mockMessageId))
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));
    }

    @Test
    @DisplayName("자신이 작성한 메시지 신고 시도 시 실패")
    void t5() throws Exception {
        // Given
        Member reporter = memberService.join("reporter5@test.com", "1234", "IT", "USER");
        createdMembers.add(reporter);
        String accessToken = memberService.genAccessToken(reporter);
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();
        ChatRoomParticipant p1 = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, reporter, "익명의 동료"));
        // 본인이 작성한 메시지
        ChatMessage ownMessage = chatMessageRepository.save(new ChatMessage(chatRoom, p1, "내가 쓴 부적절한 글"));
        UUID reportedMessageId = ownMessage.getId();
        TestTransaction.flagForCommit();
        TestTransaction.end();
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
                                            "reason": "장난 신고"
                                        }
                                        """.formatted(roomId, reportedMessageId))
                )
                .andDo(print());
        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("자신을 신고할 수 없습니다."));
    }
    @Test
    @DisplayName("동일한 메시지 중복 신고 시도 시 실패")
    void t6() throws Exception {
        // Given
        Member reporter = memberService.join("reporter6@test.com", "1234", "IT", "USER");
        Member reported = memberService.join("reported6@test.com", "1234", "IT", "USER");
        createdMembers.add(reporter);
        createdMembers.add(reported);
        String accessToken = memberService.genAccessToken(reporter);
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();
        ChatRoomParticipant p1 = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, reporter, "익명1"));
        ChatRoomParticipant p2 = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, reported, "익명2"));
        ChatMessage targetMessage = chatMessageRepository.save(new ChatMessage(chatRoom, p2, "중복 신고해봐라"));
        UUID reportedMessageId = targetMessage.getId();
        // 1차 신고 접수 (DB에 적재하기 위해 Given 단계에서 직접 등록 처리)
        reportRepository.save(new Report(reporter, reported, chatRoom, reportedMessageId, "1차 신고"));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        // When (2차 중복 신고 요청)
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/reports")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "roomId": "%s",
                                            "reportedMessageId": "%s",
                                            "reason": "2차 중복 신고"
                                        }
                                        """.formatted(roomId, reportedMessageId))
                )
                .andDo(print());
        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
                .andExpect(jsonPath("$.msg").value("이미 신고된 메시지입니다."));
    }
}
