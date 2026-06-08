package com.bnk.global.auth;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.bnk.global.util.audit.AuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 403 — 인증은 됐지만 권한 부족.
 *          → 권한 없는 API 접근 시도를 userId + IP 기준으로 추적 가능
 *          → 관리자 API 무단 접근, 역할 상승 시도 탐지에 활용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditLogger  auditLogger;   // 추가

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException e) throws IOException {

        String path = request.getRequestURI();
        String ip   = request.getRemoteAddr();

        // 현재 인증된 사용자 ID 추출 (권한 부족이므로 인증 자체는 성공한 상태)
        Long userId = resolveUserId();

        auditLogger.failure(
            AuditLogger.AUTH,
            AuditLogger.LOGIN,
            userId,
            ip,
            "권한 없는 API 접근 차단 (403): path=" + path
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
            "success", false,
            "code",    "A002",
            "message", "접근 권한이 없습니다."
        ));
    }

    /**
     * SecurityContext에서 인증된 사용자의 ID를 추출.
     * Principal 타입이 UserDetails가 아닌 경우 null 반환 (관리자 토큰 등).
     */
    private Long resolveUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails ud) {
                return ud.getUserId();
            }
        } catch (Exception ignored) { }
        return null;
    }
}