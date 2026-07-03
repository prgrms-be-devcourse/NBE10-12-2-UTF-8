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
}
