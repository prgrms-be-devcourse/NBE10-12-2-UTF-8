package com.back.domain.report.report.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.report.report.dto.ReportRequestDto;
import com.back.domain.report.report.dto.ReportResponseDto;
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.service.ReportService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "ApiV1ReportController", description = "API 신고 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
public class ApiV1ReportController {

    private final ReportService reportService;
    private final Rq rq;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "신고 접수")
    public RsData<ReportResponseDto> createReport(@RequestBody @Valid ReportRequestDto requestDto) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new com.back.global.exception.ServiceException("401-1", "로그인 후 이용해주세요.");
        }

        Report report = reportService.createReport(
                actor,
                requestDto.roomId(),
                requestDto.reportedMessageId(),
                requestDto.reason()
        );

        return new RsData<>(
                "201-1",
                "신고가 접수되었습니다.",
                new ReportResponseDto(report)
        );
    }
}