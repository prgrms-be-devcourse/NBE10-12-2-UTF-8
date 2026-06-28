package com.back.domain.member.member.controller;
import com.back.domain.member.member.dto.MemberDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "ApiV1MemberController", description = "API 회원 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
public class ApiV1MemberController {
    private final MemberService memberService;
    private final Rq rq;
    // --- 요청 DTO (기존 양식 유지 - 내부 레코드) ---
    public record MemberSignupReq(
            @NotBlank
            @Email
            @Size(min = 5, max = 50)
            String email,
            @NotBlank
            @Size(min = 4, max = 30)
            String password,
            @NotBlank
            @Size(min = 2, max = 30)
            String industry
    ) {}
    public record MemberLoginReq(
            @NotBlank
            @Email
            @Size(min = 5, max = 50)
            String email,
            @NotBlank
            @Size(min = 4, max = 30)
            String password
    ) {}
    // --- 로그인 응답 DTO (기존 양식 유지 - 내부 레코드) ---
    public record MemberLoginRes(
            String grantType,
            String accessToken,
            String refreshToken,
            int accessTokenExpiresIn
    ) {}
    // --- Endpoints ---
    @PostMapping("/signup")
    @Transactional
    @Operation(summary = "회원가입")
    public RsData<MemberDto> signup(@Valid @RequestBody MemberSignupReq req) {
        Member member = memberService.join(req.email(), req.password(), req.industry(), "USER");
        return new RsData<>(
                "201-1",
                "회원 생성 성공",
                new MemberDto(member) // dto 패키지의 MemberDto 활용
        );
    }
    @PostMapping("/login")
    @Transactional
    @Operation(summary = "로그인")
    public RsData<MemberLoginRes> login(@Valid @RequestBody MemberLoginReq req) {
        Member member = memberService.findByEmail(req.email())
                .orElseThrow(() -> new ServiceException("401-1", "존재하지 않는 이메일입니다."));
        memberService.checkPassword(member, req.password());
        String accessToken = memberService.genAccessToken(member);
        String refreshToken = memberService.genRefreshToken(member);
        rq.setCookie("accessToken", accessToken);
        return new RsData<>(
                "200-1",
                "로그인 생성 성공",
                new MemberLoginRes( // 내부 정의한 MemberLoginRes 사용
                        "Bearer",
                        accessToken,
                        refreshToken,
                        3600
                )
        );
    }

//    @GetMapping("/me")
//    @Transactional
//    @Operation(summary = "내정보")
//    public MemberWithUsernameDto me() {
//
//        Member actor = memberService.findById(rq.getActor().getId()).get();
//
//        return new MemberWithUsernameDto(actor);
//
//    }
//
//    @DeleteMapping("/logout")
//    @Operation(summary = "로그아웃")
//    public RsData<Void> logout() {
//        rq.deleteCookie("apiKey");
//
//        rq.deleteCookie("accessToken");
//
//        return new RsData<>(
//                "200-1",
//                "로그아웃 되었습니다."
//        );
//    }

}
