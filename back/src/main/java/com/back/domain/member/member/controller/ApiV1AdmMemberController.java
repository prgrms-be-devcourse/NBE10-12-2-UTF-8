package com.back.domain.member.member.controller;


import com.back.domain.member.member.dto.MemberAdmDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
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
@RequestMapping("/api/v1/adm/members")
@RequiredArgsConstructor
@Tag(name = "ApiV1AdmMemberController", description = "관리자용 API 회원 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
public class ApiV1AdmMemberController {
    private final MemberService memberService;

    @GetMapping
    @Operation(summary = "회원 다건 조회")
    public RsData<Page<MemberAdmDto>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MemberAdmDto> members = memberService.findAll(pageable)
                .map(MemberAdmDto::new);

        return new RsData<>(
                "200-1",
                "회원 다건 조회 성공",
                members
        );
    }
    @GetMapping("/{memberId}")
    @Operation(summary = "회원 단건 조회")
    public RsData<MemberAdmDto> getItem(@PathVariable UUID memberId) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        return new RsData<>(
                "200-1",
                "회원 단건 조회 성공",
                new MemberAdmDto(member)
        );
    }
}