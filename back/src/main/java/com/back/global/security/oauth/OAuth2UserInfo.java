package com.back.global.security.oauth;

import com.back.domain.member.member.entity.AuthProvider;

public interface OAuth2UserInfo {
    String getProviderId();
    AuthProvider getProvider();
    String getEmail();
}
