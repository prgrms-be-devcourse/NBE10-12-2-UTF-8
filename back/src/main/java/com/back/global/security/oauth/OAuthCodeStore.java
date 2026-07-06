package com.back.global.security.oauth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthCodeStore {
    private final ConcurrentHashMap<String, UUID> store = new ConcurrentHashMap<>();

    public String issue(UUID memberId){
        String code = UUID.randomUUID().toString();
        store.put(code, memberId);

        return code;
    }

    public UUID consume(String code){
        UUID memberId = store.remove(code);
        if(memberId == null)throw new IllegalArgumentException("유효하지 않거나 이미 사용된 code입니다.");
        return memberId;
    }


}
