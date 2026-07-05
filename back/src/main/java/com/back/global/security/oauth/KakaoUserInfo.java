package com.back.global.security.oauth;

import com.back.domain.member.member.entity.AuthProvider;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo{
    private final Map<String, Object> attributes;
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        return (String) kakaoAccount.get("email");

    }
}
