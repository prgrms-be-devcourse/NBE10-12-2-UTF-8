package com.back.domain.chat.chatRoom.controller;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1ChatRoomControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Test
    @DisplayName("채팅방 정보 조회 성공")
    void t1() throws Exception {
        // Given
        Member member = memberService.join("testuser1@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/rooms/" + roomId)
                                .cookie(new Cookie("accessToken", accessToken)) // 쿠키 주입
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("채팅방 정보 조회 성공"))
                .andExpect(jsonPath("$.data.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.maxParticipants").value(2))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 정보 조회 시 실패")
    void t2() throws Exception {
        // Given
        Member member = memberService.join("testuser2@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);
        UUID nonexistentRoomId = UUID.randomUUID();

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/rooms/" + nonexistentRoomId)
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound()) // 404 Not Found
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("참여하지 않은 채팅방 정보 조회 시 실패")
    void t3() throws Exception {
        // Given
        Member member = memberService.join("testuser3@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/rooms/" + roomId)
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isForbidden()) // 403 Forbidden
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("접근 권한이 없습니다."));
    }
}