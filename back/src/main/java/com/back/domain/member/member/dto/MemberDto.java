package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;

public record MemberDto(
        String id,
        String email,
        Industry industry
) {
    public MemberDto(String id, String email, Industry industry) {
        this.id = id;
        this.email = email;
        this.industry = industry;
    }
    public MemberDto(Member member) {
        this(
                String.valueOf(member.getId()),
                member.getEmail(),
                member.getIndustry()
        );
    }
}