package com.back.domain.report.report.controller;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.repository.ChatMessageRepository;
import com.back.domain.chat.chatRoomParticipant.entity.ChatRoomParticipant;
import com.back.domain.chat.chatRoomParticipant.repository.ChatRoomParticipantRepository;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.entity.ReportedMessage;
import com.back.domain.report.report.repository.ReportRepository;
import com.back.domain.report.report.repository.ReportedMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1AdmReportControllerTest {

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

    @Test
    @DisplayName("관리자용 신고 목록 페이징 조회 성공")
    void t1() throws Exception {
        // Given - 관리자 로그인
        String loginResponse = mvc.perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                         "email": "admin@test.com",
                                         "password": "1234"
                                    }
                                    """)
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(loginResponse)
                .path("data")
                .path("accessToken")
                .asText();

        // Given - 1건의 신고 데이터 임의 적재
        Member reporter = memberService.join("reporter_list@test.com", "1234", Industry.IT, "USER");
        Member reported = memberService.join("reported_list@test.com", "1234", Industry.IT, "USER");

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        Report report = reportRepository.save(new Report(reporter, reported, chatRoom, UUID.randomUUID(), "욕설 및 부적절 대화"));

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/admin/reports")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("page", "0")
                                .param("size", "10")
                )
                .andDo(print());

        // Then (팀의 공통 페이징 규격 검증)
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("신고 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].reportId").value(report.getId().toString()))
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    @Test
    @DisplayName("일반 회원 권한으로 관리자용 신고 API 접근 시 403 Forbidden 차단")
    void t2() throws Exception {
        // Given - 일반 회원 가입 및 로그인
        Member user = memberService.join("normal_user@test.com", "1234", Industry.IT, "USER");

        String loginResponse = mvc.perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                         "email": "normal_user@test.com",
                                         "password": "1234"
                                    }
                                    """)
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(loginResponse)
                .path("data")
                .path("accessToken")
                .asText();

        // When - 어드민 신고 API 목록 조회 요청
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/admin/reports")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then - 403 Forbidden 차단 검증
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("특정 신고의 증거 상세 대화 조회 및 가독성 라벨링 치환 검증 성공")
    void t3() throws Exception {
        // Given - 관리자 로그인
        String loginResponse = mvc.perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                         "email": "admin@test.com",
                                         "password": "1234"
                                    }
                                    """)
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(loginResponse)
                .path("data")
                .path("accessToken")
                .asText();

        // Given - 신고자, 피신고자, 참여자 계정 생성
        Member reporter = memberService.join("reporter_detail@test.com", "1234", Industry.IT, "USER");
        Member reported = memberService.join("reported_detail@test.com", "1234", Industry.IT, "USER");
        Member participant = memberService.join("participant_detail@test.com", "1234", Industry.IT, "USER");

        // 대화방 생성 및 참여자 등록
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 3));
        ChatRoomParticipant pReporter = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, reporter, "익명의 동료"));
        ChatRoomParticipant pReported = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, reported, "익명의 동료"));
        ChatRoomParticipant pParticipant = chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, participant, "익명의 동료"));

        // 시간순 메시지 적재 (1. 참여자 -> 2. 신고자 -> 3. 피신고자 최종 욕설 타겟)
        ChatMessage msg1 = chatMessageRepository.save(new ChatMessage(chatRoom, pParticipant, "어제 회의 때 말씀하셨던 그 기획서 어디 갔나요?"));
        ChatMessage msg2 = chatMessageRepository.save(new ChatMessage(chatRoom, pReporter, "메일로 이미 공유드렸으니 확인 부탁드립니다."));
        ChatMessage msg3 = chatMessageRepository.save(new ChatMessage(chatRoom, pReported, "너 일 그따구로 할 거면 사표 써라"));

        // 신고 생성 및 백업 증거 메시지 적재 (isTarget도 설정)
        Report report = reportRepository.save(new Report(reporter, reported, chatRoom, msg3.getId(), "사내 정치성 허위 비방 및 욕설"));
        
        reportedMessageRepository.save(new ReportedMessage(report, pParticipant.getMember().getId(), pParticipant.getNickname(), msg1.getContent(), msg1.getCreatedAt(), false));
        reportedMessageRepository.save(new ReportedMessage(report, pReporter.getMember().getId(), pReporter.getNickname(), msg2.getContent(), msg2.getCreatedAt(), false));
        reportedMessageRepository.save(new ReportedMessage(report, pReported.getMember().getId(), pReported.getNickname(), msg3.getContent(), msg3.getCreatedAt(), true));

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/admin/reports/" + report.getId())
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then (라벨링 정합성 및 명세서 JSON 규격 검증)
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("신고 상세 조회 성공"))
                .andExpect(jsonPath("$.data.reportId").value(report.getId().toString()))
                .andExpect(jsonPath("$.data.reportedMessages").isArray())
                // 1번째 메시지 검증 (참여자 A)
                .andExpect(jsonPath("$.data.reportedMessages[0].content").value("어제 회의 때 말씀하셨던 그 기획서 어디 갔나요?"))
                .andExpect(jsonPath("$.data.reportedMessages[0].senderLabel").value("참여자 A"))
                .andExpect(jsonPath("$.data.reportedMessages[0].isTarget").value(false))
                // 2번째 메시지 검증 (신고자)
                .andExpect(jsonPath("$.data.reportedMessages[1].content").value("메일로 이미 공유드렸으니 확인 부탁드립니다."))
                .andExpect(jsonPath("$.data.reportedMessages[1].senderLabel").value("신고자"))
                .andExpect(jsonPath("$.data.reportedMessages[1].isTarget").value(false))
                // 3번째 메시지 검증 (피신고자 - 최종 타겟)
                .andExpect(jsonPath("$.data.reportedMessages[2].content").value("너 일 그따구로 할 거면 사표 써라"))
                .andExpect(jsonPath("$.data.reportedMessages[2].senderLabel").value("피신고자"))
                .andExpect(jsonPath("$.data.reportedMessages[2].isTarget").value(true));
    }
}
