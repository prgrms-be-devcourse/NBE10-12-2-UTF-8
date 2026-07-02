package com.back.domain.member.member.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.member.repository.MemberRepository;

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


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1AdmMemberControllerTest {
    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("관리자 회원 다건 조회")
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

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/admin/members")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("page", "0")
                                .param("size", "10")
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("회원 다건 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageable.pageSize").value(10));
    }
    @Test
    @DisplayName("관리자 회원 단건 조회")
    void t2() throws Exception {
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

        // Given - 조회할 memberId 가져오기
        String membersResponse = mvc.perform(
                        get("/api/v1/admin/members")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        String memberId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(membersResponse)
                .path("data")
                .path("content")
                .get(0)
                .path("memberId")
                .asText();

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/admin/members/" + memberId)
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("회원 단건 조회 성공"))
                .andExpect(jsonPath("$.data.memberId").exists())
                .andExpect(jsonPath("$.data.email").exists())
//                .andExpect(jsonPath("$.data.industry").exists())
                .andExpect(jsonPath("$.data.isSuspended").exists())
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("관리자 권한으로 일반 회원 정지 토글 성공")
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

        // Given - 정지시킬 일반 회원 가입
        Member user = memberService.join("user_suspend_adm@test.com", "1234", com.back.domain.member.member.entity.Industry.IT, "USER");

        // When
        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/admin/members/" + user.getId() + "/suspend")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("계정 정지 상태 토글 성공"))
                .andExpect(jsonPath("$.data.memberId").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.isSuspended").value(true));
    }

    @Test
    @DisplayName("일반 회원 권한으로 어드민 정지 API 접근 시 403 Forbidden 차단")
    void t4() throws Exception {
        // Given - 일반 회원 2명 가입 및 1명 로그인
        Member user1 = memberService.join("user1_susp_adm@test.com", "1234", com.back.domain.member.member.entity.Industry.IT, "USER");
        Member user2 = memberService.join("user2_susp_adm@test.com", "1234", com.back.domain.member.member.entity.Industry.IT, "USER");

        String loginResponse = mvc.perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                         "email": "user1_susp_adm@test.com",
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

        // When
        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/admin/members/" + user2.getId() + "/suspend")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자가 자기 자신을 제재하려고 시도할 시 400 Bad Request 실패")
    void t5() throws Exception {
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

        Member admin = memberService.findByEmail("admin@test.com").orElseThrow();

        // When
        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/admin/members/" + admin.getId() + "/suspend")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("자기 자신은 제재할 수 없습니다."));
    }

    @Test
    @DisplayName("정지 처리된 유저가 API(/me) 요청 시 JWT Security 필터 단에서 즉시 403 차단")
    void t6() throws Exception {
        // Given - 일반 회원 가입 및 로그인 후, 정지 상태로 변경
        Member user = memberService.join("user_blocked_adm@test.com", "1234", com.back.domain.member.member.entity.Industry.IT, "USER");

        String loginResponse = mvc.perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                         "email": "user_blocked_adm@test.com",
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

        // 강제로 회원을 정지 상태로 DB 수정 (토글 활용)
        user.toggleSuspended();
        memberRepository.saveAndFlush(user);

        // When
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/members/me")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then
        resultActions.andExpect(status().isForbidden());
    }
}