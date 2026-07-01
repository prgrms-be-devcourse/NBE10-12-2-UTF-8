package com.back.domain.match.matchRequest.entity;

import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

// 테크스펙 FR-02 상황 목록과 반드시 동기화해야 함
public enum Situation {
    NIGHT_WORK("야근 중"),
    MEETING_BOMB("회의 폭탄"),
    OFFICE_ROMANCE_LEAK("사내 연애 폭로"),
    BOSS_BLAME("상사 억까"),
    OFFICE_POLITICS_FATIGUE("사내 정치 피로"),
    JOB_CHANGE_URGE("이직 마려움"),
    SALARY_NEGOTIATION("연봉 협상 앞둠"),
    SLACKING("몰래 루팡중"),
    OTHER("기타");

    private final String label;

    Situation(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static Situation fromLabel(String label) {
        return Arrays.stream(values())
                .filter(s -> s.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new ServiceException("400-1", "허용되지 않는 상황 값입니다."));
    }

    public static boolean isValid(String label) {
        return Arrays.stream(values()).anyMatch(s -> s.label.equals(label));
    }
}