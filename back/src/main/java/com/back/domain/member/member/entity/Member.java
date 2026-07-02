package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity {
    @Column(unique = true)
    private String email;
    private String password;
    private Industry industry;
    private String role; // "USER", "ADMIN"
    private boolean isSuspended;
    @Column(unique = true)
    private UUID refreshToken;
    private LocalDateTime refreshTokenExpiresAt;

    public Member(UUID id, String email, String role) {
        setId(id);
        this.email = email;
        this.role = role;
    }

    public Member(String email, String password, Industry industry, String role) {
        this.email = email;
        this.password = password;
        this.industry = industry;
        this.role = role;
        this.isSuspended = false;
    }

    public void updateRefreshToken(UUID refreshToken) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshToken != null
                ? LocalDateTime.now().plusMonths(1)
                : null;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role));
        return authorities;
    }
    public void updateIndustry(Industry industry) {
        this.industry = industry;
    }

    public void toggleSuspended() {
        this.isSuspended = !this.isSuspended;
    }
}