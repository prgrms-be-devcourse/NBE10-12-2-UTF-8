package com.back.domain.chat.chatRoomMessage.controller;

import com.back.domain.chat.chatRoom.entity.ChatRoom;
import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1ChatMessageControllerTest {

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
    @DisplayName("메시지 전송 성공")
    void t1() throws Exception {
        // Given
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/rooms/" + roomId + "/messages")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "오늘 진짜 야근 미쳤네요"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("메시지 생성 성공"))
                .andExpect(jsonPath("$.data.messageId").exists())
                .andExpect(jsonPath("$.data.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.data.senderNickname").value("익명의 동료"))
                .andExpect(jsonPath("$.data.content").value("오늘 진짜 야근 미쳤네요"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("비인증 사용자가 메시지 전송 시 실패")
    void t2() throws Exception {
        // Given
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        // When — 액세스 토큰 없이 요청
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/rooms/" + roomId + "/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "인증 없이 전송 시도"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방에 메시지 전송 시 실패")
    void t3() throws Exception {
        // Given
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        UUID nonExistentRoomId = UUID.randomUUID();

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/rooms/" + nonExistentRoomId + "/messages")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "존재하지 않는 방에 전송 시도"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("종료된 채팅방에 메시지 전송 시 실패")
    void t4() throws Exception {
        // Given
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        chatRoom.close(); // 채팅방 종료 처리
        UUID roomId = chatRoom.getId();

        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/rooms/" + roomId + "/messages")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "종료된 방에 전송 시도"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("종료된 채팅방에는 메시지를 보낼 수 없습니다."));
    }

    @Test
    @DisplayName("빈 내용으로 메시지 전송 시 실패")
    void t5() throws Exception {
        // Given
        Member member = memberService.join("user5@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/rooms/" + roomId + "/messages")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": ""
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("메시지 내용을 입력해주세요."));
    }

    @Test
    @DisplayName("500자 초과 메시지 전송 시 실패")
    void t6() throws Exception {
        // Given
        Member member = memberService.join("user6@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();

        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        String longContent = "a".repeat(501);

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/rooms/" + roomId + "/messages")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "%s"
                                        }
                                        """.formatted(longContent))
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
                .andExpect(jsonPath("$.msg").value("메시지는 500자를 초과할 수 없습니다."));
    }

    @Test
    @DisplayName("메시지 전체 조회 성공 - isMine: true")
    void t7() throws Exception {
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();
        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        mvc.perform(
                post("/api/v1/rooms/" + roomId + "/messages")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                   {
                                    "content": "오늘 진짜 야근 미쳤네요"
                                   }
                                """)
        );

        ResultActions resultActions = mvc.perform(
                get("/api/v1/rooms/" + roomId + "/messages")
                        .cookie(new Cookie("accessToken", accessToken))
        ).andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("메시지 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].content").value("오늘 진짜 야근 미쳤네요"))
                .andExpect(jsonPath("$.data[0].isMine").value(true));
    }

    @Test
    @DisplayName("메시지 폴링 - 타인 메시지 isMine: false")
    void t8() throws Exception {
        Member sender = memberService.join("user4@test.com", "1234", "IT", "USER");
        Member viewer = memberService.join("user5@test.com", "1234", "Finance", "USER");
        String viewerToken = memberService.genAccessToken(viewer);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();
        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, sender, "익명의 동료"));
        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, viewer, "익명의 동료"));

        String senderToken = memberService.genAccessToken(sender);
        mvc.perform(
                post("/api/v1/rooms/" + roomId + "/messages")
                        .cookie(new Cookie("accessToken", senderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {
                                        "content": "저도요...갑자기 핫픽스 떨어졌어요"
                                    }
                                """)
        );

        ResultActions result = mvc.perform(
                get("/api/v1/rooms/" + roomId + "/messages")
                        .cookie(new Cookie("accessToken", viewerToken)))
                .andDo(print());

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("메시지 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].content").value("저도요...갑자기 핫픽스 떨어졌어요"))
                .andExpect(jsonPath("$.data[0].isMine").value(false));
    }

    @Test
    @DisplayName("메시지 폴링 - after 파라미터 필터링")
    void t9() throws Exception {
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();
        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        mvc.perform(post("/api/v1/rooms/" + roomId + "/messages")
                .cookie(new Cookie("accessToken", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "content":"첫 번째"
                            }
                         """)
        );

        String future = LocalDateTime.now().plusSeconds(10)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        ResultActions result = mvc.perform(get("/api/v1/rooms/" + roomId + "/messages")
                        .param("after", future)
                        .cookie(new Cookie("accessToken", accessToken)))
                .andDo(print());

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-2"))
                .andExpect(jsonPath("$.msg").value("신규 메시지 없음"));
    }


    @Test
    @DisplayName("메시지 폴링 - 신규 메시지 없음")
    void t10() throws Exception {
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        UUID roomId = chatRoom.getId();
        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        ResultActions result = mvc.perform(get("/api/v1/rooms/" + roomId + "/messages")
                        .cookie(new Cookie("accessToken", accessToken)))
                        .andDo(print());

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-2"))
                .andExpect(jsonPath("$.msg").value("신규 메시지 없음"));
    }

    // t11: 종료된 채팅방 폴링
    @Test
    @DisplayName("메시지 폴링 - 종료된 채팅방")
    void t11() throws Exception {
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ChatRoom chatRoom = new ChatRoom(ChatRoomStatus.ACTIVE, 2);
        chatRoom.close();
        chatRoomRepository.save(chatRoom);
        UUID roomId = chatRoom.getId();

        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, member, "익명의 동료"));

        ResultActions result = mvc.perform(
                get("/api/v1/rooms/" + roomId + "/messages")
                        .cookie(new Cookie("accessToken", accessToken)))
                .andDo(print());

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-3"))
                .andExpect(jsonPath("$.msg").value("종료된 채팅방입니다."))
                .andExpect(jsonPath("$.data").value((Object) null));
    }


    @Test
    @DisplayName("메시지 폴링 - 존재하지 않는 채팅방 404")
    void t12() throws Exception {
        Member member = memberService.join("user4@test.com", "1234", "IT", "USER");
        String accessToken = memberService.genAccessToken(member);

        ResultActions result = mvc.perform(get("/api/v1/rooms/" + UUID.randomUUID() + "/messages")
                        .cookie(new Cookie("accessToken", accessToken)))
                        .andDo(print());

        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("메시지 폴링 - 비인증 사용자 401")
    void t13() throws Exception {
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));

        ResultActions result = mvc.perform(get("/api/v1/rooms/" + chatRoom.getId() + "/messages"))
                .andDo(print());

        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("메시지 폴링 - 참여자 아닌 사용자 403")
    void t14() throws Exception {
        Member owner = memberService.join("owner@test.com", "1234", "IT", "USER");
        Member outsider = memberService.join("outsider@test.com", "1234", "IT", "USER");
        String outsiderToken = memberService.genAccessToken(outsider);

        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(ChatRoomStatus.ACTIVE, 2));
        chatRoomParticipantRepository.save(new ChatRoomParticipant(chatRoom, owner, "익명의 동료"));

        ResultActions result = mvc.perform(
                        get("/api/v1/rooms/" + chatRoom.getId() + "/messages")
                                .cookie(new Cookie("accessToken", outsiderToken)))
                .andDo(print());

        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }
}