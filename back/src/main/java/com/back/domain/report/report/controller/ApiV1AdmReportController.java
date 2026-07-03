package com.back.domain.report.report.controller;

import com.back.domain.report.report.dto.ReportAdmDetailDto;
import com.back.domain.report.report.dto.ReportAdmDto;
import com.back.domain.report.report.dto.ReportStatusUpdateDto;
import com.back.domain.report.report.service.ReportService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "ApiV1AdmReportController", description = "관리자용 API 신고 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
public class ApiV1AdmReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "신고 목록 조회")
    public RsData<Page<ReportAdmDto>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportAdmDto> reports = reportService.getReportsForAdmin(pageable)
                .map(ReportAdmDto::new);

        return new RsData<>(
                "200-1",
                "신고 목록 조회 성공",
                reports
        );
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "신고 상세 조회")
    public RsData<ReportAdmDetailDto> getItem(@PathVariable UUID reportId) {
        ReportAdmDetailDto reportDetail = reportService.getReportDetailForAdmin(reportId);

        return new RsData<>(
                "200-1",
                "신고 상세 조회 성공",
                reportDetail
        );
    }

    @PatchMapping("/{reportId}/status")
    @Operation(summary = "신고서 처리 상태 수정")
    public RsData<ReportStatusUpdateDto> toggleStatus(@PathVariable UUID reportId) {
        ReportStatusUpdateDto statusUpdate = reportService.toggleReportStatus(reportId);

        return new RsData<>(
                "200-1",
                "신고서 처리 상태 수정 성공",
                statusUpdate
        );
    }
}