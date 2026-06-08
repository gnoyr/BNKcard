package com.bnk.global.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bnk.global.util.audit.AuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [보안 패치] 2026-06-08
 *
 * 변경 1 — XFF 스푸핑 방어
 *   변경 전: X-Forwarded-For 헤더를 무조건 신뢰 → 공격자가 IP를 매 요청마다 위조 가능
 *   변경 후: application.properties의 server.forward-headers-strategy=framework 설정과
 *            Spring의 ForwardedHeaderFilter 가 신뢰 프록시에서만 XFF를 수용하도록 처리.
 *            이 필터에서는 RemoteAddr만 사용 (프록시 레이어가 이미 실제 IP로 교체한 값).
 *
 *   운영 환경 Nginx 설정 추가 필수:
 *     proxy_set_header X-Forwarded-For $remote_addr;  # overwrite, not append
 *     proxy_set_header X-Real-IP $remote_addr;
 *
 * 변경 2 — Rate Limit 적용 경로 확장
 *   변경 전: /api/auth/login, /api/admin/auth/login 만 적용
 *   변경 후: 이메일 인증 발송, 비밀번호 재설정 등 고비용 API 전체로 확장
 *            경로별로 MAX_REQUESTS 차등 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    // ── Rate Limit 정책 ──────────────────────────────────────────────
    // 1분 윈도우 기준 최대 허용 요청 수
    private static final Duration WINDOW = Duration.ofMinutes(1);

    /**
     * 경로별 Rate Limit 정책.
     * key: URI prefix (완전 일치), value: 1분 내 최대 허용 횟수
     */
    private static final Map<String, Long> RATE_LIMIT_RULES = Map.of(
			"/api/auth/login", 10L, // 로그인: 1분 10회
			"/api/admin/auth/login", 10L, // 관리자 로그인: 1분 10회
			"/api/auth/send-verify-code", 5L, // 이메일 인증 발송: 1분 5회
			"/api/auth/verify-email", 10L, // 인증코드 확인: 1분 10회
			"/api/auth/find-password", 5L, // 비밀번호 재설정 요청: 1분 5회
			"/api/auth/reset-password", 5L, // 비밀번호 재설정: 1분 5회
			"/api/auth/signup", 3L // 회원가입: 1분 3회
    );

    /** Rate Limit을 적용할 HTTP 메서드 */
    private static final Set<String> TARGET_METHODS = Set.of("POST");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // 대상 메서드가 아닌 경우 패스
        if (!TARGET_METHODS.contains(method.toUpperCase())) {
            chain.doFilter(request, response);
            return;
        }

        // 적용 경로 확인
        Long maxRequests = getMaxRequests(path);
        if (maxRequests == null) {
            chain.doFilter(request, response);
            return;
        }

        // XFF를 신뢰하지 않고 RemoteAddr만 사용
        // Nginx가 proxy_set_header X-Forwarded-For $remote_addr 로 덮어쓴 뒤
        // Spring의 ForwardedHeaderFilter가 RemoteAddr를 실제 IP로 교체함
        String ip  = resolveClientIp(request);
        String key = buildRedisKey(path, ip);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count > maxRequests) {
            log.warn("[RateLimit] 요청 초과 path={} ip={} count={} limit={}", path, ip, count, maxRequests);
            auditLogger.failure(AuditLogger.AUTH, AuditLogger.LOGIN,
                    null, ip, "Rate Limit 초과 - 1분 내 " + count + "회 시도 (" + path + ")");
            writeTooManyRequests(response);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 경로에 맞는 최대 허용 횟수 반환. 해당 경로가 없으면 null.
     */
    private Long getMaxRequests(String path) {
        return RATE_LIMIT_RULES.get(path);
    }

    /**
     * Redis 키 생성: "rate:{path-segment}:{ip}"
     * 예) rate:login:1.2.3.4, rate:send-verify-code:5.6.7.8
     */
    private String buildRedisKey(String path, String ip) {
        // URI 마지막 세그먼트를 키 식별자로 사용
        String segment = path.substring(path.lastIndexOf('/') + 1);
        return "rate:" + segment + ":" + ip;
    }

    /**
     * XFF 헤더를 신뢰하지 않음.
     * server.forward-headers-strategy=framework + ForwardedHeaderFilter 조합으로
     * Spring이 신뢰 프록시의 XFF만 적용하여 RemoteAddr를 실제 클라이언트 IP로 교체함.
     * 따라서 여기서는 RemoteAddr만 읽으면 됨.
     *
     * 로컬 개발 편의를 위해 IPv6 루프백(::1)만 변환.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(remote) ? "127.0.0.1" : remote;
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(response.getWriter(),
                Map.of("success", false, "code", "C004",
                       "message", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."));
    }
}