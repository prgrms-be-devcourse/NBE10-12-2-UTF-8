package com.back.domain.member.member.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum AuthProvider {
    LOCAL,
    GOOGLE,
    KAKAO
}
