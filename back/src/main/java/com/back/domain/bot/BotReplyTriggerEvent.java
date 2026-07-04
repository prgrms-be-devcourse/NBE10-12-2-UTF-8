package com.back.domain.bot;

import java.util.UUID;

// 매칭 성사 직후(오프닝 멘트) 또는 사람이 메시지를 보낸 직후(맥락 답장) 둘 다에 쓰이는 이벤트
public record BotReplyTriggerEvent(UUID roomId, UUID botMemberId) {}
