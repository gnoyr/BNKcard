package com.bnk.global.util.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 시스템 감사 로그 유틸.
 *
 * ── 동작 ─────────────────────────────────────────────────────────────
 *   1) Slf4j 로그 출력 (log.info / log.warn)
 *   2) category에 따라 DB 테이블 자동 라우팅 INSERT
 *
 * ── 테이블 라우팅 ─────────────────────────────────────────────────────
 *   AUTH                               → AUDIT_LOGS         (기존 테이블)
 *   CARD_APPLY / TERMS / FILE / USER   → USER_ACTIVITY_LOG  (신규)
 *   CARD / CDD / ADMIN / EXTERNAL_API  → ADMIN_ACTIVITY_LOG (신규)
 *
 * ── 변경 이력 ────────────────────────────────────────────────────────
 *   - USER 카테고리 추가 (관리자에 의한 회원 관리 행위 → ADMIN_ACTIVITY_LOG)
 *   - HttpServletRequest 주입 → clientIp, requestUri 자동 수집
 *   - WATCHLIST 카테고리 추가 → ADMIN_ACTIVITY_LOG 라우팅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogMapper    auditLogMapper;
    private final HttpServletRequest httpRequest;   // ✅ IP/URI 자동 수집용

    // ── 카테고리 상수 ─────────────────────────────────────────────────

    /** 보안 이벤트 → AUDIT_LOGS */
    public static final String AUTH = "AUTH";

    /** 사용자 행위 → USER_ACTIVITY_LOG */
    public static final String CARD_APPLY = "CARD_APPLY";
    public static final String TERMS      = "TERMS";
    public static final String FILE       = "FILE";

    /** 관리자 행위 → ADMIN_ACTIVITY_LOG */
    public static final String CARD         = "CARD";
    public static final String CDD          = "CDD";
    public static final String ADMIN        = "ADMIN";
    public static final String EXTERNAL_API = "EXTERNAL_API";
    public static final String USER         = "USER";   
    public static final String WATCHLIST    = "WATCHLIST"; 

    // ── 액션 상수 ─────────────────────────────────────────────────────
    public static final String LOGIN           = "LOGIN";
    public static final String LOGOUT          = "LOGOUT";
    public static final String SIGNUP          = "SIGNUP";
    public static final String WITHDRAW        = "WITHDRAW";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String EMAIL_VERIFY    = "EMAIL_VERIFY";
    public static final String TOKEN_REFRESH   = "TOKEN_REFRESH";
    public static final String SYSTEM_ERROR    = "SYSTEM_ERROR";

    public static final String APPLY        = "APPLY";
    public static final String APPLY_CANCEL = "APPLY_CANCEL";
    public static final String AGREE        = "AGREE";
    public static final String UPLOAD       = "UPLOAD";
    public static final String DOWNLOAD     = "DOWNLOAD";

    public static final String CREATE           = "CREATE";
    public static final String UPDATE           = "UPDATE";
    public static final String STATUS_CHANGE    = "STATUS_CHANGE";
    public static final String APPROVAL_REQUEST = "APPROVAL_REQUEST";
    public static final String APPROVAL_APPROVE = "APPROVAL_APPROVE";
    public static final String APPROVAL_REJECT  = "APPROVAL_REJECT";
    public static final String API_CALL         = "API_CALL";
    public static final String CDD_STATUS_CHANGE   = "CDD_STATUS_CHANGE";
    public static final String WATCHLIST_REGISTER  = "WATCHLIST_REGISTER";
    public static final String WATCHLIST_REMOVE    = "WATCHLIST_REMOVE";

    // ─────────────────────────────────────────────────────────────────
    // 편의 메서드
    // ─────────────────────────────────────────────────────────────────

    /** 사용자 성공 로그 */
    public void success(String category, String action,
                        Long userId, String targetId, String detail) {
        record(category, action, "S", userId, null, targetId, detail);
    }

    /** 사용자 실패 로그 */
    public void failure(String category, String action,
                        Long userId, String targetId, String detail) {
        record(category, action, "F", userId, null, targetId, detail);
    }

    /** 관리자 성공 로그 */
    public void adminSuccess(String category, String action,
                             Long adminId, String targetId, String detail) {
        record(category, action, "S", null, adminId, targetId, detail);
    }

    /** 관리자 실패 로그 */
    public void adminFailure(String category, String action,
                             Long adminId, String targetId, String detail) {
        record(category, action, "F", null, adminId, targetId, detail);
    }

    // ─────────────────────────────────────────────────────────────────
    // 핵심: Slf4j 로그 + category에 따라 테이블 라우팅
    // ─────────────────────────────────────────────────────────────────

    private void record(String category, String action, String result,
                        Long userId, Long adminId, String targetId, String detail) {

        // ① Slf4j 로그
        String actor = adminId != null ? "adminId=" + adminId
                     : userId  != null ? "userId="  + userId
                     : "actor=-";

        String msg = String.format("[AUDIT] %s | %s | %s | %s | target=%s | %s",
                "S".equals(result) ? "SUCCESS" : "FAILURE",
                category, action, actor,
                targetId != null ? targetId : "-",
                detail   != null ? detail   : "-");

        if ("S".equals(result)) log.info(msg);
        else                    log.warn(msg);

        // IP, URI 자동 수집
        String clientIp  = resolveClientIp();
        String requestUri = resolveRequestUri();

        // ② DB INSERT — 실패해도 본 비즈니스 로직에 영향 없도록 try-catch
        try {
            if (AUTH.equals(category)) {
                Long   actorId   = adminId != null ? adminId : userId;
                String actorType = adminId != null ? "ADMIN" : "USER";
                auditLogMapper.insertSecurityLog(
                        actorType, actorId, action,
                        null, null, detail, clientIp);

            } else if (isAdminCategory(category)) {
                // USER, WATCHLIST 포함한 관리자 행위 → ADMIN_ACTIVITY_LOG
                auditLogMapper.insertAdminActivity(
                        AdminActivityLog.builder()
                                .adminId(adminId)
                                .action(category + "_" + action)
                                .result(result)
                                .targetId(targetId)
                                .detail(detail)
                                .clientIp(clientIp)
                                .requestUri(requestUri)
                                .build());
            } else {
                // 사용자 행위 → USER_ACTIVITY_LOG
                auditLogMapper.insertUserActivity(
                        UserActivityLog.builder()
                                .userId(userId)
                                .action(category + "_" + action)
                                .result(result)
                                .targetId(targetId)
                                .detail(detail)
                                .clientIp(clientIp)
                                .requestUri(requestUri)
                                .build());
            }
        } catch (Exception e) {
            log.error("[AUDIT] DB 저장 실패 — category={} action={} result={}", category, action, result, e);
        }
    }

    /**
     * USER, WATCHLIST 카테고리 추가
     * - USER     : 관리자가 회원 상태 변경, 잠금 해제 등 회원 관리 시
     * - WATCHLIST: 요주의 인물 등록/삭제 시
     */
    private boolean isAdminCategory(String category) {
        return CARD.equals(category)
            || CDD.equals(category)
            || ADMIN.equals(category)
            || EXTERNAL_API.equals(category)
            || USER.equals(category)
            || WATCHLIST.equals(category);
    }

    /**
     * server.forward-headers-strategy=framework 설정으로
     *    Spring이 XFF를 이미 처리 → getRemoteAddr()만 사용
     */
    private String resolveClientIp() {
        try {
            String ip = httpRequest.getRemoteAddr();
            return ip != null && ip.length() > 100 ? ip.substring(0, 100) : ip;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveRequestUri() {
        try {
            String uri = httpRequest.getRequestURI();
            return uri != null && uri.length() > 200 ? uri.substring(0, 200) : uri;
        } catch (Exception ignored) {
            return null;
        }
    }
}