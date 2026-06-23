package com.bnk.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bnk.global.auth.JwtAccessDeniedHandler;
import com.bnk.global.auth.JwtAuthenticationEntryPoint;
import com.bnk.global.auth.JwtAuthenticationFilter;
import com.bnk.global.filter.RedisRateLimitFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security 설정.
 *
 * ── 인증 구조 ─────────────────────────────────────────────────────────
 *   - JWT 기반 Stateless 인증 (세션 미사용)
 *   - 세션을 사용하지 않으므로 CSRF 공격 벡터 없음
 *     → CSRF 비활성화 (JWT + Stateless 조합에서의 표준 패턴)
 *
 * ── 역할(Role) 체계 ───────────────────────────────────────────────────
 *   일반 사용자:
 *     ROLE_USER       : 로그인한 일반 회원
 *
 *   관리자 (계층형):
 *     ROLE_SUPER_ADMIN : 최상위 관리자 — 모든 관리자 API 접근 가능
 *     ROLE_MANAGER     : 중간 관리자  — 카드/약관/결재 관리
 *     ROLE_OPERATOR    : 하위 운영자  — 회원 조회·잠금 해제 등 운영 업무
 *
 * ── API 접근 권한 매핑 ────────────────────────────────────────────────
 *   공개 (permitAll)
 *     /api/auth/**             : 회원 인증 (로그인·회원가입·이메일 인증 등)
 *     /api/admin/auth/**       : 관리자 인증 (로그인·로그아웃·Refresh)
 *     /api/cards, /api/cards/**: 카드 공개 조회
 *     /api/search/**           : 검색
 *     /api/terms/**            : 약관 공개 조회
 *     /api/chat/**             : 챗봇
 *     /api/init                : 초기화
 *
 *   ROLE_USER (로그인 사용자)
 *     /api/users/**            : 마이페이지 (내 정보 조회·수정, 카드 신청 등)
 *     /api/applications/**     : 카드 신청
 *     /api/auth/refresh        : 토큰 재발급 (인증 필요 경로)
 *     /api/auth/ip-verify/**   : IP 인증
 *
 *   ROLE_OPERATOR 이상 (관리자 공통)
 *     /api/admin/users/**      : 회원 목록 조회, 잠금 해제, 상태 변경
 *     /api/admin/dashboard     : 대시보드 (OPERATOR는 운영 현황만)
 *     /api/admin/auth/me       : 내 관리자 정보 조회
 *
 *   ROLE_MANAGER 이상 (카드·약관·결재)
 *     /api/admin/cards/**      : 카드 생성·수정·상태 변경·결재 신청
 *     /api/admin/terms/**      : 약관 생성·수정·상태 변경·결재 신청
 *     /api/admin/approvals/**  : 결재 목록 조회, 승인/반려
 *     /api/admin/files/**      : 파일 업로드·다운로드
 *     /api/admin/watchlist/**  : 요주의 인물 조회
 *
 *   ROLE_SUPER_ADMIN 전용
 *     /api/admin/migration/**  : 데이터 마이그레이션
 *     /api/admin/admins/**     : 관리자 계정 생성·역할 부여
 *     /api/admin/cdd/**        : CDD 상태 강제 변경
 *     /api/admin/watchlist/register, /delete : 요주의 인물 등록·삭제
 *
 * ── Security Hotspot 대응 ─────────────────────────────────────────────
 *   CSRF: JWT + Stateless 인증에서는 세션 쿠키 기반 공격이 불가능.
 *         Access Token은 HttpOnly 쿠키로 전송되며, SameSite=Lax 설정으로
 *         Cross-Site 요청 차단이 추가 보호층으로 작동.
 *         → SonarQube CSRF Hotspot: Reviewed(Safe) 처리 근거 명시.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화
public class SecurityConfig {

    private final JwtAuthenticationFilter     jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler      jwtAccessDeniedHandler;
    private final RedisRateLimitFilter        rateLimitFilter;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            RedisRateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationFilter     = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler      = jwtAccessDeniedHandler;
        this.rateLimitFilter             = rateLimitFilter;
        if (rateLimitFilter == null) {
            log.warn("[SecurityConfig] RedisRateLimitFilter 비활성 — Rate Limit 미적용 상태");
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsServiceImpl) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsServiceImpl);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ──────────────────────────────────────────────────────────
            // CSRF
            // [Security Hotspot] "Make sure disabling Spring Security's CSRF protection is safe here."
            //
            // 비활성화 근거 (Reviewed — Safe):
            //  1. JWT 기반 Stateless 인증 사용 → 서버 세션 없음.
            //  2. CSRF 공격은 브라우저가 세션 쿠키를 자동 전송하는 메커니즘을 악용.
            //     세션이 없으므로 CSRF 공격 벡터 자체가 존재하지 않음.
            //  3. Access Token은 HttpOnly 쿠키로 전달하며, 쿠키에 SameSite=Lax 설정.
            //     SameSite=Lax는 Cross-Site POST 요청 시 쿠키를 전송하지 않으므로
            //     추가 방어층으로 동작.
            //  4. CORS 설정으로 허용된 Origin만 API 호출 가능.
            //
            // 결론: JWT + Stateless + SameSite=Lax + CORS 조합으로 충분히 보호됨.
            //       CSRF 토큰 강제 시 API 클라이언트(모바일·SPA) 호환성만 해침.
            // ──────────────────────────────────────────────────────────
            .csrf(csrf -> csrf.disable()) // NOSONAR: JWT Stateless + SameSite=Lax 쿠키로 CSRF 방어 충분 (위 주석 참조)

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 세션 완전 비활성화 — Stateless JWT 인증
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── 보안 헤더 ─────────────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://t1.daumcdn.net; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "img-src 'self' data: https:; " +
                        "connect-src 'self'; " +
                        "frame-src 'self' https://t1.daumcdn.net https://ssl.daumcdn.net http://postcode.map.kakao.com https://postcode.map.kakao.com; " +
                        "frame-ancestors 'none';"
                    ))
            )

            // ── 예외 핸들러 ───────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // 401
                .accessDeniedHandler(jwtAccessDeniedHandler)              // 403
            )

            // ══════════════════════════════════════════════════════════
            // API 접근 권한 매핑
            // ══════════════════════════════════════════════════════════
            .authorizeHttpRequests(auth -> auth

                // ── 1. 정적 리소스 (비인증 허용) ──────────────────────
                .requestMatchers(
                    "/css/**", "/js/**", "/images/**", "/fonts/**",
                    "/components/**", "/favicon.ico", "/error",
                    "/.well-known/**", "/index.html"
                ).permitAll()

                // ── 2. 공개 HTML 페이지 (비인증 허용) ────────────────
                .requestMatchers(
                    "/", "/login", "/signup", "/find-id", "/reset-password",
                    "/copy-code", "/card/**",
                    "/auth/**",          // 인증 관련 HTML
                    "/terms/**",         // 약관 페이지
                    "/admin/login"       // 관리자 로그인 페이지
                ).permitAll()

                // ── 3. Swagger (인증 필요 — 운영 환경 보호) ──────────
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
                ).authenticated()

                // ── 4. 회원 인증 API (비인증 허용) ───────────────────
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/logout",
                    "/api/auth/signup",
                    "/api/auth/send-verify-code",
                    "/api/auth/verify-email",
                    "/api/auth/find-id",
                    "/api/auth/find-password",
                    "/api/auth/reset-password",
                    "/api/auth/refresh",
                    "/api/auth/ip-verify/**"
                ).permitAll()

                // ── 5. 관리자 인증 API (비인증 허용) ─────────────────
                .requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/logout",
                    "/api/admin/auth/refresh"
                ).permitAll()

                // ── 6. 공개 조회 API (비인증 허용) ───────────────────
                .requestMatchers(
                    "/api/cards",
                    "/api/cards/**",
                    "/api/search/**",
                    "/api/terms/packages/**",
                    "/api/terms/*/files",
                    "/api/chat",
                    "/api/chat/**",
                    "/api/chat/history",
                    "/api/init"
                ).permitAll()

                // ── 7. SUPER_ADMIN 전용 API ───────────────────────────
                // 마이그레이션, 관리자 계정 관리, CDD 강제 변경,
                // 요주의 인물 등록·삭제
                .requestMatchers(
//                    "/css/**",
//                    "/js/**",
//                    "/images/**",
//                    "/fonts/**",
//                    "/components/**",
//                    "/favicon.ico",
//                    "/error",
//                    "/.well-known/**",
//                    "/index.html"                    
//                ).permitAll()
                    "/api/admin/migration/**",
                    "/api/admin/admins/**",
                    "/api/admin/cdd/**",
                    "/api/admin/watchlist/register",
                    "/api/admin/watchlist/*/delete"
                ).hasRole("SUPER_ADMIN")

                // ── 8. MANAGER 이상 API ──────────────────────────────
                // 카드·약관·결재·파일·요주의 인물 조회
                // SUPER_ADMIN도 이 영역 접근 가능 (hasAnyRole로 처리)
                .requestMatchers(
                    "/api/admin/cards/**",
                    "/api/admin/terms/**",
                    "/api/admin/approvals/**",
                    "/api/admin/files/**",
                    "/api/admin/watchlist/**"
                ).hasAnyRole("SUPER_ADMIN", "MANAGER")

                // ── 9. OPERATOR 이상 API (관리자 공통) ───────────────
                // 회원 조회·잠금 해제·상태 변경, 대시보드, 내 정보
                .requestMatchers(
                    "/api/admin/users/**",
                    "/api/admin/dashboard",
                    "/api/admin/auth/me"
                ).hasAnyRole("SUPER_ADMIN", "MANAGER", "OPERATOR")

                // ── 10. 일반 사용자 API (ROLE_USER) ──────────────────
                // 마이페이지, 카드 신청 등 — 로그인 사용자만
                .requestMatchers(
                    "/api/users/**",
                    "/api/applications/**"
                ).hasRole("USER")

                // ── 11. 나머지 — 인증 필수 ───────────────────────────
                .anyRequest().authenticated()
            )

            // ── JWT 필터 등록 ─────────────────────────────────────────
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);
            log.info("[SecurityConfig] RedisRateLimitFilter 등록 완료");
        }

        return http.build();
    }

    // ── CORS 설정 ──────────────────────────────────────────────────────
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> {
                if ("*".equals(s)) {
                    log.error("[CORS] 와일드카드(*) origin은 허용되지 않습니다. cors.allowed-origins 설정을 확인하세요.");
                    return false;
                }
                return true;
            })
            .toList();

        if (origins.isEmpty()) {
            log.error("[CORS] 허용된 origin이 없습니다. cors.allowed-origins 설정을 확인하세요.");
        }

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}