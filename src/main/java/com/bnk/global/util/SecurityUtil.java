package com.bnk.global.util;

import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.auth.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * SecurityContextHolder에서 현재 인증 정보 추출.
 * 서비스 레이어에서 사용 시 활용. 컨트롤러는 @AuthenticationPrincipal 권장.
 */
public class SecurityUtil {

    private SecurityUtil() {}

    public static Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof CustomUserDetails ud) {
            return Optional.of(ud.getUserId());
        }
        return Optional.empty();
    }

    public static Optional<Long> getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof CustomAdminDetails ad) {
            return Optional.of(ad.getAdminId());
        }
        return Optional.empty();
    }
}
