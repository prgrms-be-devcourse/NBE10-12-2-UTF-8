package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.standard.util.Ut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthTokenService {
    @Value("${custom.accessToken.expirationSeconds}")
    private int expireSeconds;

    @Value("${custom.jwt.secretKey}")
    private String secret;

    public String genAccessToken(Member member) {
        String id = member.getId().toString();
        String email = member.getEmail();
        String role = member.getRole();

        return Ut.jwt.toString(
                secret,
                expireSeconds,
                Map.of("id", id, "email", email, "role", role)
        );
    }

    public Map<String, Object> payload(String accessToken) {
        Map<String, Object> parsedPayload = Ut.jwt.payload(secret, accessToken);

        if (parsedPayload == null) return null;

        UUID id = UUID.fromString((String) parsedPayload.get("id"));
        String email = (String) parsedPayload.get("email");
        String role = (String) parsedPayload.get("role");

        return Map.of("id", id, "email", email, "role", role);
    }
}