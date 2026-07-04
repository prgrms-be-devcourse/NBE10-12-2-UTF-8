package com.back.domain.bot;

import java.util.List;

// 봇이 채팅방에서 자동으로 보낼 문구 목록.
// 실제 상황(situation)에 안 맞아도 자연스럽게 들리도록 범용적인 공감 문구로 구성.
// 자유롭게 추가/수정해도 됨.
public final class BotReplyMessages {
    private BotReplyMessages() {}

    public static final List<String> LINES = List.of(
            "오 저도 완전 공감돼요 ㅋㅋ",
            "무슨 일 있으셨어요?",
            "아 진짜 힘드셨겠다...",
            "저도 비슷한 일 겪은 적 있어서 더 와닿네요",
            "그래도 오늘 하루 고생 많으셨어요!",
            "저만 이런 거 아니었네요 ㅎㅎ",
            "그거 완전 스트레스일 것 같아요",
            "잠깐 이렇게 얘기하니까 좀 낫네요"
    );
}