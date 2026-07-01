package com.back.global.initData;

import com.back.domain.member.member.service.MemberService;
import com.back.global.app.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import static com.back.domain.member.member.entity.Industry.*;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    @Autowired
    @Lazy
    private BaseInitData self;
    private final MemberService memberService;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.work1();
        };
    }

    @Transactional
    public void work1() {
        if (memberService.count() > 0) return;

        AppConfig.isDev();

        // 시스템 및 관리자 계정 생성 (이메일 및 어드민 권한 매개변수 적용)
        memberService.join("admin@test.com", "1234", null, "ADMIN");

        // 일반 유저 초기화 (이메일, 비밀번호, 업계코드, USER 권한 적용)
        memberService.join("user1@test.com", "1234", IT, "USER");
        memberService.join("user2@test.com", "1234", OFFICE, "USER");
        memberService.join("user3@test.com", "1234", FINANCE, "USER");
    }
}