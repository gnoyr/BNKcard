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

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	/**
	 * application-local.properties 의 cors.allowed-origins 값을 주입받는다. 쉼표(,)로 구분된 복수
	 * 도메인을 지원한다.
	 *
	 * 로컬 예시 : http://localhost:8088,http://localhost:8089 운영 예시 : https://도메인.store
	 */
	@Value("${cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * UserDetailsService 빈이 2개(userDetailsServiceImpl, adminDetailsServiceImpl)이므로
	 * 일반 유저용을 명시적으로 지정하여 WARN 제거. JWT 쿠키 인증 구조에서 이 빈이 직접 호출되진 않으나, Spring Security
	 * Global AuthenticationManager의 모호성을 해소한다.
	 */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsServiceImpl) {
		// Spring Security 7.x: DaoAuthenticationProvider(UserDetailsService) 생성자 사용
		// UserDetailsService를 명시하여 2개
		// 빈(userDetailsServiceImpl/adminDetailsServiceImpl)의 모호성 제거
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsServiceImpl);
		return new ProviderManager(provider);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(jwtAccessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						// 모든 요청(API, index.html, static 자원 전체)을 인증 없이 무조건 허용
						.anyRequest().permitAll())
				// 일단 필터도 우회 가능하도록 순서는 유지하되 위의 permitAll()이 우선 적용됩니다.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// 쉼표 구분 → 트림 → List 변환
		List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.toList();
		configuration.setAllowedOrigins(origins);
		configuration.setAllowedHeaders(List.of("*"));

		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

		// 쿠키(HttpOnly) 전송을 허용하기 위해 true 필수
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}