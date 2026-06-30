package com.back.domain.dashboard.dashboard.controller;

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
public class ApiV1DashboardControllerTest {
    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("관리자 대시보드 통계 조회")
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
                        get("/api/v1/adm/dashboard")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("대시보드 통계 조회 성공"))
                .andExpect(jsonPath("$.data.matchStatistics.totalMembers").exists())
                .andExpect(jsonPath("$.data.matchStatistics.todayMatches").exists())
                .andExpect(jsonPath("$.data.matchStatistics.activeChatRooms").exists())
                .andExpect(jsonPath("$.data.industryStatistics").isArray());
    }
}