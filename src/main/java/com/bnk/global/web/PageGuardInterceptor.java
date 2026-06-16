package com.bnk.global.web;

import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.auth.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 페이지(HTML) 레벨 접근 제어 인터셉터.
 *
 * SecurityConfig는 API 레벨만 보호하므로, 정적 HTML 페이지 경로에 대한
 * role 검증은 이 인터셉터가 담당한다.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ Guard 종류 및 접근 허용 범위                                        │
 * │  USER_ONLY   : 로그인한 일반 사용자만 (관리자 차단)                  │
 * │  OPERATOR    : 세 관리자 role 모두 허용 (SUPER_ADMIN ≥ MANAGER ≥ OPERATOR) │
 * │  MANAGER     : SUPER_ADMIN, MANAGER만 허용                       │
 * │  SUPER_ADMIN : SUPER_ADMIN만 허용                                 │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * 미허가 접근 시 동작:
 *  - 비로그인        → 각 도메인 로그인 페이지로 redirect
 *  - 일반 사용자 → 관리자 페이지 : /login 으로 redirect
 *  - 관리자 → 사용자 페이지       : /admin/login 으로 redirect
 *  - Role 불일치 관리자           : 본인 role 기본 페이지로 redirect
 */
@Slf4j
public class PageGuardInterceptor implements HandlerInterceptor {

    public enum Guard {
        USER_ONLY,   // 일반 사용자 전용
        OPERATOR,    // 관리자 공통 (OPERATOR 이상)
        MANAGER,     // MANAGER 이상
        SUPER_ADMIN  // SUPER_ADMIN 전용
    }

    private final Guard guard;

    public PageGuardInterceptor(Guard guard) {
        this.guard = guard;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = isAuthenticated(auth);

        // ── 1. 비로그인 처리 ─────────────────────────────────────────
        if (!isAuthenticated) {
            if (guard == Guard.USER_ONLY) {
                log.debug("[PageGuard] 비로그인 → /login : {}", request.getRequestURI());
                response.sendRedirect("/login");
            } else {
                log.debug("[PageGuard] 비로그인(관리자) → /admin/login : {}", request.getRequestURI());
                response.sendRedirect("/admin/login");
            }
            return false;
        }

        Object principal = auth.getPrincipal();

        // ── 2. 관리자가 사용자 페이지 접근 ──────────────────────────
        if (guard == Guard.USER_ONLY && principal instanceof CustomAdminDetails ad) {
            log.warn("[PageGuard] 관리자(adminId={})가 사용자 페이지 접근 차단: {}",
                    ad.getAdminId(), request.getRequestURI());
            response.sendRedirect(resolveAdminHome(auth));
            return false;
        }

        // ── 3. 일반 사용자가 관리자 페이지 접근 ─────────────────────
        if (guard != Guard.USER_ONLY && principal instanceof CustomUserDetails ud) {
            log.warn("[PageGuard] 사용자(userId={})가 관리자 페이지 접근 차단: {}",
                    ud.getUserId(), request.getRequestURI());
            response.sendRedirect("/login");
            return false;
        }

        // ── 4. 관리자 role별 페이지 접근 제어 ───────────────────────
        if (principal instanceof CustomAdminDetails ad) {
            boolean allowed = switch (guard) {
                case SUPER_ADMIN -> hasRole(auth, "ROLE_SUPER_ADMIN");
                case MANAGER     -> hasRole(auth, "ROLE_SUPER_ADMIN")
                                 || hasRole(auth, "ROLE_MANAGER");
                case OPERATOR    -> hasRole(auth, "ROLE_SUPER_ADMIN")
                                 || hasRole(auth, "ROLE_MANAGER")
                                 || hasRole(auth, "ROLE_OPERATOR");
                default          -> true;
            };

            if (!allowed) {
                String dest = resolveAdminHome(auth);
                log.warn("[PageGuard] Role 불일치 — adminId={}, uri={} → {}",
                        ad.getAdminId(), request.getRequestURI(), dest);
                response.sendRedirect(dest);
                return false;
            }
        }

        return true;
    }

    // ── 인증 여부 확인 ────────────────────────────────────────────────
    private boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    // ── role 포함 여부 확인 ───────────────────────────────────────────
    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities()
                   .contains(new SimpleGrantedAuthority(role));
    }

    /**
     * 현재 관리자 role에 맞는 기본 랜딩 페이지 반환.
     * role이 없는 경우 /admin/login으로 fallback.
     */
    private String resolveAdminHome(Authentication auth) {
        if (hasRole(auth, "ROLE_SUPER_ADMIN")) return "/admin/approvals";
        if (hasRole(auth, "ROLE_MANAGER"))     return "/admin/cards";
        if (hasRole(auth, "ROLE_OPERATOR"))    return "/admin/users";
        return "/admin/login";
    }
}