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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
                .andExpect(jsonPath("$.msg").value("신고가 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").exists())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }
}