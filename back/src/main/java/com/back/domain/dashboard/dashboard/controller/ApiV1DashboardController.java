package com.back.domain.dashboard.dashboard.controller;

import com.back.domain.dashboard.dashboard.dto.DashboardResponseDto;
import com.back.domain.dashboard.dashboard.service.DashboardService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/adm/dashboard")
@RequiredArgsConstructor
@Tag(name = "ApiV1DashboardController", description = "관리자 대시보드 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
public class ApiV1DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "대시보드 통계 조회")
    public RsData<DashboardResponseDto> getDashboard() {
        return new RsData<>(
                "200-1",
                "대시보드 통계 조회 성공",
                dashboardService.getDashboard()
        );
    }
}