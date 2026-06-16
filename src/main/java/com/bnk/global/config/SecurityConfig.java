package com.bnk.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
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

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter      jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler       jwtAccessDeniedHandler;
    private final RedisRateLimitFilter         rateLimitFilter;

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
            // ── CSRF ────────────────────────────────────────────────────
            // JWT 기반 Stateless 인증 → 세션 미사용 → CSRF 공격 벡터 없음
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/**")
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))

            // ── HTTP Security Headers ─────────────────────────────────
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://t1.daumcdn.net; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "img-src 'self' data: " +
                            "https://bnkcard.store " +
                            "https://www.bnkcard.store " +
                            "https://objectstorage.ap-chuncheon-1.oraclecloud.com " +
                            "https://www.busanbank.co.kr " +
                            "https://busanbank.co.kr; " +
                        "connect-src 'self' https://t1.daumcdn.net; " +
                        "frame-src 'self' https://t1.daumcdn.net http://postcode.map.kakao.com https://postcode.map.kakao.com " +
                            "https://objectstorage.ap-chuncheon-1.oraclecloud.com; " +
                        "object-src 'self' https://objectstorage.ap-chuncheon-1.oraclecloud.com; " +
                        "frame-ancestors 'none';"
                    ))
            )

            .authorizeHttpRequests(auth -> auth

                // ── 회원 인증 API ─────────────────────────────────────────
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

                // ── 관리자 인증 API ───────────────────────────────────────
                .requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/logout",
                    "/api/admin/auth/refresh"
                ).permitAll()

                // ── 카드 공개 조회 (비로그인 허용) ───────────────────────
                .requestMatchers(
                    "/api/cards",
                    "/api/cards/**",
                    "/api/search/**",
                    "/api/terms/packages/**",
                    "/api/terms/*/files",
                    "/api/chat/history",
                    "/api/init"
                ).permitAll()

                // ── 약관 페이지 (비로그인 허용) ──────────────────────────
                .requestMatchers("/terms/**").permitAll()

                // ── 정적 리소스 (CSS / JS / 이미지 / 폰트 등) ────────────
                // ⚠ HTML 파일 경로(/admin/**, /mypage/**)는 의도적으로 제외.
                //    페이지 접근 제어는 WebMvcConfig > PageGuardInterceptor 가 담당.
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",
                    "/components/**",
                    "/favicon.ico",
                    "/error",
                    "/.well-known/**"
                ).permitAll()

                // ── 인증 관련 HTML 페이지 (로그인·회원가입 등 — 비로그인 허용) ──
                // /auth/** 하위 정적 HTML은 누구나 접근 가능해야 함
                .requestMatchers(
                    "/auth/**",
                    "/auth/ip-verify"
                ).permitAll()

                // ── 공개 페이지 (비로그인 허용) ──────────────────────────
                .requestMatchers(
                    "/",
                    "/login",
                    "/signup",
                    "/find-id",
                    "/reset-password",
                    "/copy-code",
                    "/card/**"
                ).permitAll()

                // ── 관리자 로그인 페이지 (비로그인 허용) ─────────────────
                .requestMatchers("/admin/login").permitAll()

                // ── Swagger — 인증 필요 (운영 차단) ─────────────────────
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).authenticated()

                // ── 관리자 API — 역할 필수 ───────────────────────────────
                .requestMatchers("/api/admin/**")
                .hasAnyRole("SUPER_ADMIN", "MANAGER", "OPERATOR")

                // ── 나머지 — 로그인 필수 ────────────────────────────────
                // /mypage/**, /admin/cards, /admin/users 등 보호 페이지는
                // Spring Security 통과 후 PageGuardInterceptor가 role 검증
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);
            log.info("[SecurityConfig] RedisRateLimitFilter 등록 완료");
        }

        return http.build();
    }

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

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedHeaders(List.of(
            "Content-Type",
            "Authorization",
            "Cookie",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));
        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}