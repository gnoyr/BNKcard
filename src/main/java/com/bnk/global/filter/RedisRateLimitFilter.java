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
 * 로그인 Rate Limiting 필터.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
public class RedisRateLimitFilter extends OncePerRequestFilter {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final AuditLogger auditLogger;

	private static final String LOGIN_PATH = "/api/auth/login";
	private static final String ADMIN_LOGIN_PATH = "/api/admin/auth/login";
	private static final long MAX_REQUESTS = 10L;
	private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!LOGIN_PATH.equals(path) && !ADMIN_LOGIN_PATH.equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String ip  = resolveClientIp(request);
        String key = "rate:login:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count > MAX_REQUESTS) {
            log.warn("[RateLimit] 로그인 초과 ip={} count={}", ip, count);
            auditLogger.failure(AuditLogger.AUTH, AuditLogger.LOGIN,
                    null, ip, "Rate Limit 초과 - 1분 내 " + count + "회 시도");
            writeTooManyRequests(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
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