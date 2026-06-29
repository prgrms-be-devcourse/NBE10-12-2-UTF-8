package com.back.domain.member.member.controller;

import com.back.domain.member.member.service.MemberService;

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
public class ApiV1MemberControllerTest {
    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("회원가입")
    void t1() throws Exception {
        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                             "email": "test@test.com",
                                             "password": "1234",
                                             "industry": "IT"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("회원 생성 성공"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.industry").value("IT"));
    }

    @Test
    @DisplayName("로그인")
    void t2() throws Exception {
        // Given - 회원가입 선행
        mvc.perform(
                post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                     "email": "test@test.com",
                                     "password": "1234",
                                     "industry": "IT"
                                }
                                """)
        );

        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                             "email": "test@test.com",
                                             "password": "1234"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그인 생성 성공"))
                .andExpect(jsonPath("$.data.grantType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.accessTokenExpiresIn").exists());
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식으로 회원가입 시 실패")
    void t3() throws Exception {
        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                             "email": "invalid-email-format",
                                             "password": "1234",
                                             "industry": "IT"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("가입되지 않은 이메일로 로그인 시 실패")
    void t4() throws Exception {
        // When
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                             "email": "nonexistent@test.com",
                                             "password": "1234"
                                        }
                                        """)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isUnauthorized());
    }
}
