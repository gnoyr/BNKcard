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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final AdminDetailsServiceImpl adminDetailsService;

    /** 필터를 완전히 건너뛸 공개 경로 */
    private static final List<String> SKIP_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/signup",
        "/api/auth/verify-email",
        "/api/auth/find-id",
        "/api/auth/find-password",
        "/api/auth/reset-password",
        "/api/admin/auth/login",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1) SKIP_PATHS 에 매칭되는 주소는 토큰 검사 없이 바로 통과
        if (SKIP_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveAccessToken(request);

        // 2) 유효한 토큰이 제공된 경우 Spring Security 인증 객체 등록
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String type = jwtTokenProvider.getTokenType(token);

            // REFRESH 타입 토큰이 access_token 쿠키에 잘못 세팅된 경우를 방어.
            if (!"ACCESS".equals(type) && !"ADMIN_ACCESS".equals(type)) {
                log.warn("[JWT Filter] 잘못된 토큰 타입이 access_token 쿠키에서 감지됨: type={}, path={}",
                        type, path);
                setAnonymous();
                chain.doFilter(request, response);
                return;
            }

            Long subjectId = jwtTokenProvider.getUserId(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                if ("ADMIN_ACCESS".equals(type)) {
                    // DB 조회 결과가 아닌 토큰 클레임의 역할을 권위 있는 소스로 사용.
                    UserDetails adminDetails = adminDetailsService.loadUserById(subjectId);
                    List<GrantedAuthority> authorities = jwtTokenProvider.getRoleList(token)
                            .stream()
                            .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            adminDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                } else {
                    // 일반 사용자 토큰 (type = "ACCESS")
                    UserDetails userDetails = userDetailsService.loadUserById(subjectId);
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

        } else {
            // 3) 토큰이 없거나 유효하지 않은 비로그인 사용자
            // Spring Security 기본 "anonymousUser" 문자열 대신 명시적 익명 인증 객체를 등록.
            // → 컨트롤러 @AuthenticationPrincipal 주입 타입 미스매치 방어
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                setAnonymous();
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 익명 인증 객체를 SecurityContext에 등록.
     * 비로그인 / 잘못된 토큰 타입 두 경우 모두 공통 사용.
     */
    private void setAnonymous() {
        AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
            "anonymousKey",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

    /**
     * 토큰 탐색 — access_token 쿠키에서 추출
     */
    private String resolveAccessToken(HttpServletRequest request) {
        return CookieUtil.extractCookieValue(request, "access_token").orElse(null);
    }
}
