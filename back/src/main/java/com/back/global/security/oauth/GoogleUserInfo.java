package com.back.global.security.oauth;

import com.back.domain.member.member.entity.AuthProvider;

import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo{
    private final Map<String, Object> attributes;
    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    public String getProviderId() {
        return String.valueOf(attributes.get("sub"));
    }
    public String getEmail() {
        return (String) attributes.get("email");
    }
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }
}
