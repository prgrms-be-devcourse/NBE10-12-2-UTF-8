package com.back.global.security.oauth;

import com.back.global.exception.ServiceException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthCodeStore {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private record CodeEntry(UUID memberId, Instant issuedAt) {
        boolean isExpired() {
            return Instant.now().isAfter(issuedAt.plus(CODE_TTL));
        }
    }

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

    public String issue(UUID memberId) {
        String code = UUID.randomUUID().toString();
        store.put(code, new CodeEntry(memberId, Instant.now()));
        return code;
    }

    public UUID consume(String code) {
        CodeEntry entry = store.remove(code);
        if (entry == null || entry.isExpired()) {
            throw new ServiceException("400-1", "유효하지 않거나 이미 사용된 code입니다.");
        }
        return entry.memberId();
    }

    // exchange 요청 없이 버려진 code가 메모리에 계속 쌓이는 것을 방지
    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
