package com.bnk.global.auth;

import com.bnk.global.util.audit.AuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 401 — 미인증 요청.
 *          → 인증 없이 보호된 API에 접근하는 시도를 IP 기준으로 추적 가능
 *
 * 주의: 정상 UX 흐름(프론트엔드가 401을 받아 refresh 재시도)에서도 호출됨.
 *       과도한 DB INSERT를 막기 위해 /api/auth/** 경로는 로깅 제외.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final AuditLogger  auditLogger;   // 추가

    /** 프론트엔드 refresh 흐름에서 정상적으로 401이 발생하는 경로 — 감사 로그 제외 */
    private static final java.util.Set<String> SKIP_AUDIT_PATHS = java.util.Set.of(
        "/api/auth/refresh",
        "/api/users/me"       // 헤더 로그인 상태 확인용 polling
    );

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException e) throws IOException {

        String path = request.getRequestURI();
        String ip   = request.getRemoteAddr();

        // 정상 UX 흐름 경로가 아닌 경우에만 감사 로그 기록
        if (!SKIP_AUDIT_PATHS.contains(path)) {
            auditLogger.failure(
                AuditLogger.AUTH,
                AuditLogger.LOGIN,
                null,
                ip,
                "미인증 접근 차단 (401): path=" + path
            );
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
            "success", false,
            "code",    "A001",
            "message", "인증이 필요합니다."
        ));
    }
}