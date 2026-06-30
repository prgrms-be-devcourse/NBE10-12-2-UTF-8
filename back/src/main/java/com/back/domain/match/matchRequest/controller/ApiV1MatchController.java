package com.back.domain.match.matchRequest.controller;

import com.back.domain.match.matchRequest.dto.MatchRequestDto;
import com.back.domain.match.matchRequest.dto.MatchResponseDto;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.service.MatchRequestService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "ApiV1MatchController", description = "API 매칭 컨트롤러")
public class ApiV1MatchController {
    private final MatchRequestService matchRequestService;
    private final Rq rq;

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "매칭 요청 생성")
    public RsData<MatchResponseDto> create(
            @RequestBody @Valid MatchRequestDto dto
    ) {
        Member actor = memberService.findById(rq.getActor().getId())
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));
        MatchRequest matchRequest = matchRequestService.create(actor, dto.situation());

        return new RsData<>(
                "201-1",
                "매칭 요청 생성 성공",
                MatchResponseDto.ofCreated(matchRequest)
        );

    }

        @GetMapping("/{matchRequestId}")
        public RsData<MatchResponseDto> getMatchRequest(
                @PathVariable UUID matchRequestId
        ) {
            MatchRequest matchRequest = matchRequestService.findById(matchRequestId);


    }
}
