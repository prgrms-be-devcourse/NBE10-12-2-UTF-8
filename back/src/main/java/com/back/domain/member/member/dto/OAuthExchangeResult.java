package com.back.domain.member.member.dto;

import java.util.UUID;

public record OAuthExchangeResult(
        String accessToken,
        UUID refreshToken,
        boolean needsOnboarding
) {}
