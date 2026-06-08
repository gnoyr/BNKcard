package com.bnk.global.util;

import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.global.util.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 보안 처리 유틸.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenSecurityService {

    private final UserSessionMapper userSessionMapper;
    private final AuditLogger       auditLogger;

    /**
     * revoke된 토큰 재사용 감지 → 해당 userId 전 기기 세션 강제 만료.
     * AUDIT_LOGS에 FAILURE 기록 (AUTH 카테고리).
     */
    @Transactional
    public void handleStolenToken(String refreshToken) {
        userSessionMapper.findAnyByRefreshToken(refreshToken).ifPresent(session -> {
            Long userId = session.getUserId();
            int  count  = userSessionMapper.revokeAllByUserId(userId);

            log.warn("[TokenSecurity] Refresh Token 재사용 감지 — 탈취 의심. userId={} | 전 기기 세션 {}건 강제 만료",
                    userId, count);

            auditLogger.failure(AuditLogger.AUTH, AuditLogger.TOKEN_REFRESH,
                    userId, null, "Refresh Token 재사용 감지 — 전 기기 세션 강제 만료");
        });
    }

    /**
     * 비밀번호 변경 후 현재 세션 외 전 기기 로그아웃.
     * revokeOtherSessions() — UserSessionMapper 기존 메서드 사용.
     */
    @Transactional
    public void revokeOtherSessions(Long userId, Long currentSessionId) {
        int count = userSessionMapper.revokeOtherSessions(userId, currentSessionId);
        if (count > 0) {
            log.info("[TokenSecurity] 비밀번호 변경 — 타 기기 세션 {}건 만료. userId={}", count, userId);
            auditLogger.success(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
                    userId, null, "비밀번호 변경 — 타 기기 세션 " + count + "건 강제 만료");
        }
    }
}