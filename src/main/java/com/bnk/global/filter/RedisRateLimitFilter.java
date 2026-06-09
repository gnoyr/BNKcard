package com.bnk.global.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

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
 * IP 기반 Rate Limit 필터 (Redis Sliding Window 근사).
 *
 * [보안 패치] 2026-06-08
 *   변경 1 — XFF 스푸핑 방어: RemoteAddr 전용
 *   변경 2 — POST 인증 API Rate Limit 경로 확장
 *   변경 3 — GET 공개 API Rate Limit 추가 (/api/cards/**, /api/search/**)
 *   변경 4 — Redis 장애 시 fail-closed (503 반환)
 *
 *  auditLogger.failure(AUTH, LOGIN) 추가
 *            → Redis 인프라 장애 이벤트를 AUDIT_LOGS DB에서 추적 가능
 * resolveAuditAction()으로 경로·메서드에 맞는 액션 상수 반환
 *            POST 인증 경로 → LOGIN / SIGNUP / EMAIL_VERIFY 등
 *            GET 공개 경로 → API_CALL (EXTERNAL_API 카테고리 신규 활용)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;
    private final AuditLogger         auditLogger;

    private static final Duration WINDOW = Duration.ofMinutes(1);

    // ── POST 인증 API ─────────────────────────────────────────────────
    private static final Map<String, Long> POST_RATE_LIMIT_RULES = Map.of(
            "/api/auth/login",            10L,
            "/api/admin/auth/login",      10L,
            "/api/auth/send-verify-code",  5L,
            "/api/auth/verify-email",     10L,
            "/api/auth/find-password",     5L,
            "/api/auth/reset-password",    5L,
            "/api/auth/signup",            3L
    );

    // ── GET 공개 API ──────────────────────────────────────────────────
    private static final Map<String, Long> GET_RATE_LIMIT_RULES = Map.of(
            "/api/cards",  60L,
            "/api/search", 60L
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod().toUpperCase();
        String ip     = resolveClientIp(request);

        Long maxRequests = resolveMaxRequests(path, method);
        if (maxRequests == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = buildRedisKey(path, method, ip);

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }

            if (count != null && count > maxRequests) {
                String action = resolveAuditAction(path, method);
                log.warn("[RateLimit] 요청 초과 method={} path={} ip={} count={} limit={}",
                        method, path, ip, count, maxRequests);
                auditLogger.failure(
                        AuditLogger.AUTH,
                        action,
                        null,
                        ip,
                        "Rate Limit 초과: " + method + " " + path
                                + " | 1분 내 " + count + "회 (최대 " + maxRequests + "회)"
                );
                writeTooManyRequests(response);
                return;
            }

        } catch (Exception e) {
            log.error("[RateLimit] Redis 연결 오류 — fail-closed 적용: path={} ip={} error={}",
                    path, ip, e.getMessage());
            auditLogger.failure(
                    AuditLogger.AUTH,
                    AuditLogger.LOGIN,
                    null,
                    ip,
                    "Redis 장애로 Rate Limit 불가 (fail-closed 503): path=" + path
                            + " | error=" + e.getMessage()
            );
            writeServiceUnavailable(response);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 경로·메서드 조합으로 최대 허용 횟수 반환.
     * POST: 완전 일치, GET: prefix 매칭. 해당 없으면 null.
     */
    private Long resolveMaxRequests(String path, String method) {
        if ("POST".equals(method)) {
            return POST_RATE_LIMIT_RULES.get(path);
        }
        if ("GET".equals(method)) {
            return GET_RATE_LIMIT_RULES.entrySet().stream()
                    .filter(e -> path.startsWith(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     *   POST /api/auth/signup         → SIGNUP
     *   POST /api/auth/send-verify-code, verify-email → EMAIL_VERIFY
     *   POST /api/auth/login, admin login → LOGIN
     *   POST /api/auth/find-password, reset-password → PASSWORD_CHANGE
     *   GET  공개 API                 → API_CALL
     */
    private String resolveAuditAction(String path, String method) {
        if ("GET".equals(method))              return AuditLogger.API_CALL;
        if (path.contains("signup"))           return AuditLogger.SIGNUP;
        if (path.contains("verify"))           return AuditLogger.EMAIL_VERIFY;
        if (path.contains("password"))         return AuditLogger.PASSWORD_CHANGE;
        return AuditLogger.LOGIN;  // /api/auth/login, /api/admin/auth/login
    }

    /**
     * Redis 키 생성: "rate:{method}:{path-segment}:{ip}"
     * 예) rate:POST:login:1.2.3.4, rate:GET:cards:5.6.7.8
     */
    private String buildRedisKey(String path, String method, String ip) {
        String segment = path.substring(path.lastIndexOf('/') + 1);
        return "rate:" + method + ":" + segment + ":" + ip;
    }

    /**
     * XFF 헤더를 신뢰하지 않음.
     * server.forward-headers-strategy=framework + ForwardedHeaderFilter 조합으로
     * Spring이 신뢰 프록시의 XFF만 적용하여 RemoteAddr를 실제 클라이언트 IP로 교체함.
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

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setHeader("Retry-After", "30");
        objectMapper.writeValue(response.getWriter(),
                Map.of("success", false, "code", "S001",
                        "message", "서비스가 일시적으로 불가합니다. 잠시 후 다시 시도해 주세요."));
    }
}