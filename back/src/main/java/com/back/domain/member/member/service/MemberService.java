package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    public long count() {
        return memberRepository.count();
    }

    public Member join(String email, String password, String industry, String role) {
        findByEmail(email).ifPresent(_ -> {
            throw new ServiceException("409-1", "이미 존재하는 이메일입니다.");
        });

        String encodedPassword = passwordEncoder.encode(password);
        Member member = new Member(email, encodedPassword, industry, role);

        return memberRepository.save(member);
    }

    public void checkPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public String genAccessToken(Member member) {
        return authTokenService.genAccessToken(member);
    }

    public UUID genRefreshToken(Member member) {
        UUID token = UUID.randomUUID();
        member.updateRefreshToken(token);
        return token;
    }

    public Map<String, Object> payload(String accessToken) {
        return authTokenService.payload(accessToken);
    }

    public Optional<Member> findById(UUID id) {
        return memberRepository.findById(id);
    }
}