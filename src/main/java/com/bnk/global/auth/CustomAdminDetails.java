package com.bnk.global.auth;

import com.bnk.domain.admin.model.AdminUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자 인증 객체.
 * 컨트롤러에서 사용 예시:
 *   @AuthenticationPrincipal CustomAdminDetails ad
 *   Long adminId = ad.getAdminId();
 */
@Getter
public class CustomAdminDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final AdminUser adminUser;

    public CustomAdminDetails(AdminUser adminUser) {
        this.adminUser = adminUser;
    }

    /** 컨트롤러에서 adminId 추출용 핵심 메서드 */
    public Long getAdminId() {
        return adminUser.getAdminId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<String> roleCodes = adminUser.getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return roleCodes.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
    }

    @Override public String getPassword() { return adminUser.getPasswordHash(); }
    @Override public String getUsername() { return adminUser.getUsername(); }

    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDED".equals(adminUser.getStatusCode());
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
