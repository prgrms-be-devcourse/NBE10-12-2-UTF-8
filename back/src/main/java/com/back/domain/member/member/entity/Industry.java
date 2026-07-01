package com.back.domain.member.member.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.back.global.exception.ServiceException;

import java.util.Arrays;

// 테크스펙 FR-02 산업군 목록과 반드시 동기화해야 함
public enum Industry {
    IT("IT/개발"),
    SERVICE("서비스업"),
    FINANCE("금융업"),
    MEDICAL("의료서비스"),
    RETAIL("유통"),
    MEDIA("미디어/디자인"),
    OFFICE("사무업");

    private final String label;

    Industry(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static Industry fromLabel(String label) {
        return Arrays.stream(values())
                .filter(i -> i.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new ServiceException("400-1", "허용되지 않는 산업군입니다."));
    }

    public static boolean isValid(String label) {
        return Arrays.stream(values()).anyMatch(i -> i.label.equals(label));
    }
}