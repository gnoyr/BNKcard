package com.bnk.global.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

        // 2) 유효한 토큰이 제공된 경우 정상적으로 Spring Security 인증 객체 등록
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String type = jwtTokenProvider.getTokenType(token);
            Long subjectId = jwtTokenProvider.getUserId(token);

            UserDetails userDetails;
            if ("ADMIN_ACCESS".equals(type)) {
                userDetails = adminDetailsService.loadUserById(subjectId);
            } else {
                userDetails = userDetailsService.loadUserById(subjectId);
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } else {
            // 3) 토큰이 아예 없거나 잘못된 비로그인 사용자인 경우 (핵심 수정 대목)
            // 시큐리티 기본 문자열 "anonymousUser" 대신 익명 사용자를 명확히 컨텍스트에 등록하여
            // 컨트롤러의 @AuthenticationPrincipal 단에서 주입 타입 미스매치로 인하여 튕기는 현상을 방어합니다.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                    "anonymousKey", 
                    "anonymousUser", 
                    List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                );
                SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 토큰 탐색
     * access_token 쿠키 확인
     */
    private String resolveAccessToken(HttpServletRequest request) {
        return CookieUtil.extractCookieValue(request, "access_token").orElse(null);
    }
}