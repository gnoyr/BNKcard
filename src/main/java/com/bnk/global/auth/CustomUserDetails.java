package com.bnk.global.auth;

import com.bnk.domain.user.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 일반 사용자 인증 객체.
 * 컨트롤러에서 사용 예시:
 *   @AuthenticationPrincipal CustomUserDetails ud
 *   Long userId = ud.getUserId();
 *
 * 비로그인 허용 API:
 *   @AuthenticationPrincipal(required = false) CustomUserDetails ud
 *   Long userId = ud != null ? ud.getUserId() : null;
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /** 컨트롤러에서 userId 추출용 핵심 메서드 */
    public Long getUserId() {
        return user.getUserId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getEmail(); }

    @Override
    public boolean isAccountNonLocked() {
        String status = user.getStatusCode();
        return !"SUSPENDED".equals(status) && !"WITHDRAWN".equals(status);
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return !"DORMANT".equals(user.getStatusCode());
    }
}
