package com.bnk.global.auth;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.audit.AuditLogger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Access Token 검증 필터.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider       jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final AdminDetailsServiceImpl adminDetailsService;
    private final AuditLogger            auditLogger;   // 추가

    /**
     * 필터를 완전히 건너뛸 공개 경로.
     *
     * 로그아웃은 토큰이 만료된 상태에서도 호출될 수 있음.
     * SKIP_PATHS 에 포함해야 필터가 토큰 검사 없이 통과시키고
     * 컨트롤러의 @AuthenticationPrincipal(errorOnInvalidType = false) 가
     * ud=null 로 처리해 쿠키만 삭제하는 정상 흐름이 동작함.
     * SecurityConfig.permitAll() 만으로는 부족 — 필터가 먼저 실행되기 때문.
     */
    private static final List<String> SKIP_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/logout",
        "/api/auth/signup",
        "/api/auth/send-verify-code",
        "/api/auth/verify-email",
        "/api/auth/find-id",
        "/api/auth/find-password",
        "/api/auth/reset-password",
        "/api/admin/auth/login",
        "/api/admin/auth/logout",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/api/auth/ip-verify/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1) SKIP_PATHS 에 매칭되는 경로는 토큰 검사 없이 바로 통과
        if (SKIP_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveAccessToken(request);

        // 2) 유효한 토큰이 있는 경우 Spring Security 인증 객체 등록
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String type = jwtTokenProvider.getTokenType(token);

            // REFRESH 타입 토큰이 access_token 쿠키에 잘못 세팅된 경우 방어:
            // "ACCESS" / "ADMIN_ACCESS" 외 타입은 익명 처리 후 감사 로그 기록
            if (!"ACCESS".equals(type) && !"ADMIN_ACCESS".equals(type)) {
                String ip = request.getRemoteAddr();

                // AUDIT_LOGS에 FAILURE 이벤트 INSERT — 토큰 위조·오남용 추적용
                auditLogger.failure(
                    AuditLogger.AUTH,
                    AuditLogger.TOKEN_REFRESH,
                    null,
                    ip,
                    "허용되지 않은 토큰 타입 감지: type=" + type + ", path=" + path
                );

                setAnonymous();
                chain.doFilter(request, response);
                return;
            }

            Long subjectId = jwtTokenProvider.getUserId(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                if ("ADMIN_ACCESS".equals(type)) {
                    UserDetails adminDetails = adminDetailsService.loadUserById(subjectId);
                    List<GrantedAuthority> authorities = jwtTokenProvider.getRoleList(token)
                            .stream()
                            .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(
                                    r.startsWith("ROLE_") ? r : "ROLE_" + r))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            adminDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                } else {
                    UserDetails userDetails = userDetailsService.loadUserById(subjectId);
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

        } else {
            // 3) 토큰이 없거나 유효하지 않은 경우 — 익명 인증 객체 등록
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                setAnonymous();
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 익명 인증 객체를 SecurityContext 에 등록.
     * 비로그인 / 허용되지 않은 토큰 타입 두 경우 모두 사용.
     */
    private void setAnonymous() {
        AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
            "anonymousKey",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

    /** 토큰 탐색 — access_token 쿠키에서 추출 */
    private String resolveAccessToken(HttpServletRequest request) {
        return CookieUtil.extractCookieValue(request, "access_token").orElse(null);
    }
}