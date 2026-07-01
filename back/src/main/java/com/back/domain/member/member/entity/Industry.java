package com.back.domain.member.member.entity;

import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final Map<String, Industry> LABEL_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(i -> i.label, i -> i));

    @JsonCreator
    public static Industry fromLabel(String label) {
        Industry result = LABEL_MAP.get(label);
        if (result == null) {
            throw new ServiceException("400-1", "허용되지 않는 산업군입니다.");
        }
        return result;
    }
}