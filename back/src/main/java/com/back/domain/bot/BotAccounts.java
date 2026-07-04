package com.back.domain.bot;

import com.back.domain.member.member.entity.Industry;

import java.util.UUID;

// 지인 테스트용 봇 계정 관련 상수/유틸.
// 산업군마다 봇 하나씩 만들어서, 실제 유저가 매칭 요청을 넣으면 즉시 매칭되게 한다.
// 더 이상 필요 없어지면 이 bot 패키지 전체 + BaseInitData/MatchRequestService의
// 연동 코드만 지우면 깔끔하게 제거된다.
public final class BotAccounts {
    private BotAccounts() {}

    private static final String EMAIL_PREFIX = "bot.";
    private static final String EMAIL_DOMAIN = "@tangbisil.bot";

    // 봇 계정은 아무도 로그인할 일이 없어서 비밀번호 값 자체는 의미 없음
    public static final String PASSWORD = UUID.randomUUID().toString();

    public static String emailFor(Industry industry) {
        return EMAIL_PREFIX + industry.name().toLowerCase() + EMAIL_DOMAIN;
    }

    public static boolean isBotEmail(String email) {
        return email != null && email.startsWith(EMAIL_PREFIX) && email.endsWith(EMAIL_DOMAIN);
    }
}