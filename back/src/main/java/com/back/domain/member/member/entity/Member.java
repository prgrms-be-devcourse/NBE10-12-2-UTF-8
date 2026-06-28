package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity {
    @Column(unique = true)
    private String email;
    private String password;
    private String industry;
    private String role; // "USER", "ADMIN"
    private boolean isSuspended;
    private String refreshToken;

    // 인증/인가 시 사용하는 결합용 생성자
    public Member(int id, String email, String role) {
        setId(id);
        this.email = email;
        this.role = role;
    }

    public Member(String email, String password, String industry, String role) {
        this.email = email;
        this.password = password;
        this.industry = industry;
        this.role = role;
        this.isSuspended = false;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role));
        return authorities;
    }
}