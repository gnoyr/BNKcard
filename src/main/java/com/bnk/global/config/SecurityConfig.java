package com.bnk.global.config;

import java.util.Arrays;
import java.util.List;

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

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * UserDetailsService 빈이 2개(userDetailsServiceImpl, adminDetailsServiceImpl)이므로
     * 일반 유저용을 명시적으로 지정하여 WARN 제거.
     * JWT 쿠키 인증 구조에서 이 빈이 직접 호출되진 않으나,
     * Spring Security Global AuthenticationManager의 모호성을 해소한다.
     */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsServiceImpl) {
        // Spring Security 7.x: DaoAuthenticationProvider(UserDetailsService) 생성자 사용
        // UserDetailsService를 명시하여 2개 빈(userDetailsServiceImpl/adminDetailsServiceImpl)의 모호성 제거
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsServiceImpl);
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
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // 모든 요청(API, index.html, static 자원 전체)을 인증 없이 무조건 허용
                .anyRequest().permitAll()
            )
            // 일단 필터도 우회 가능하도록 순서는 유지하되 위의 permitAll()이 우선 적용됩니다.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
    	CorsConfiguration configuration = new CorsConfiguration();
        // 쿠키를 주고받으려면 AllowedOriginPatterns에 "*"를 단독으로 쓰면 브라우저 보안 정책상 차단될 수 있습니다.
        // 프론트엔드가 구동되는 실제 도메인(예: http://localhost:8080 등)을 명확히 지정해주는 것이 안전합니다.
        configuration.setAllowedOrigins(List.of("http://localhost:8088")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 쿠키 전송을 허용하기 위해 true 설정 필수 유지
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}