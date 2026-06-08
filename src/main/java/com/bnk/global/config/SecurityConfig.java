package com.bnk.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bnk.global.auth.JwtAccessDeniedHandler;
import com.bnk.global.auth.JwtAuthenticationEntryPoint;
import com.bnk.global.auth.JwtAuthenticationFilter;
import com.bnk.global.filter.RedisRateLimitFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * [보안 패치] 2026-06-08
 *
 * 변경 전: anyRequest().permitAll() → Spring Security 인가가 사실상 미사용
 * 변경 후: 공개 경로만 명시적으로 permitAll(), 나머지는 authenticated() 요구
 *
 * 관리자 API(/api/admin/**)는 ROLE_SUPER_ADMIN, ROLE_MANAGER, ROLE_OPERATOR 중
 * 하나 이상의 역할을 가진 인증된 사용자만 접근 가능.
 *
 * CORS allowedHeaders 범위 축소:
 * 변경 전: List.of("*") — 모든 헤더 허용
 * 변경 후: 실제 사용하는 헤더만 명시
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final RedisRateLimitFilter rateLimitFilter;

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
            log.info("[SecurityConfig] RedisRateLimitFilter 비활성 (Redis disabled 또는 미설정)");
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
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth

                // ── 인증 없이 접근 가능한 공개 경로 ──────────────────────────
                // 회원 인증 (로그인, 회원가입, 이메일 인증, 비밀번호 재설정 등)
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/signup",
                    "/api/auth/send-verify-code",
                    "/api/auth/verify-email",
                    "/api/auth/find-id",
                    "/api/auth/find-password",
                    "/api/auth/reset-password",
                    "/api/auth/refresh"
                ).permitAll()

                // 관리자 인증
                .requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/refresh"
                ).permitAll()

                // 카드 공개 조회 (비로그인 사용자도 카드 목록·상세 조회 가능)
                .requestMatchers(
                    "/api/cards",
                    "/api/cards/**",
                    "/api/search/**",
                    "/api/terms/packages/**"
                ).permitAll()

                // 정적 리소스
                .requestMatchers(
                    "/",
                    "/*.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",
                    "/components/**",
                    "/admin/**",
                    "/auth/**",
                    "/mypage/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // ── 관리자 API — 관리자 역할 필수 ─────────────────────────────
                // JwtAuthenticationFilter 에서 ROLE_SUPER_ADMIN 등 관리자 권한이 부여됨
                .requestMatchers("/api/admin/**")
                    .hasAnyRole("SUPER_ADMIN", "MANAGER", "OPERATOR",
                                "CARD_MANAGER", "REVIEWER")

                // ── 나머지 모든 API — 로그인 필수 ─────────────────────────────
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
            // 와일드카드(*) origin은 credentials=true와 함께 사용 불가 — 기동 시 차단
            .filter(s -> {
                if ("*".equals(s)) {
                    log.error("[CORS] 와일드카드(*) origin은 허용되지 않습니다. cors.allowed-origins 설정을 확인하세요.");
                    return false;
                }
                return true;
            })
            .toList();

        configuration.setAllowedOrigins(origins);

        // 와일드카드 → 실제 사용 헤더만 명시
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