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
 *   IP 주소는 개인정보(개인정보보호법·GDPR 대상)이므로
 *   Slf4j 로그(콘솔/파일)에 직접 기록하지 않는다.
 *   IP는 DB의 AES 암호화 컬럼(AUDIT_LOGS.ip_address, ACTIVITY_LOG.client_ip)에만 저장.
 *   SonarQube: "Make sure that this logger's configuration does not log sensitive data."
 *   → IP를 로그 메시지에서 제거함으로써 Reviewed(Safe) 처리 가능.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogMapper     auditLogMapper;
    private final HttpServletRequest  httpRequest;

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

    // ── 편의 메서드 ────────────────────────────────────────────────────

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

    // ── 핵심: Slf4j 로그 + 테이블 라우팅 ─────────────────────────────

    private void record(String category, String action, String result,
                        Long userId, Long adminId, String targetId, String detail) {

        if (userId == null && adminId == null) {
            log.debug("[AUDIT] actor null — 스킵: {}/{}", category, action);
            return;
        }

        String actor = adminId != null ? "adminId=" + adminId
                     : "userId="  + userId;
        
        String msg = String.format("[AUDIT] %s | %s | %s | %s | target=%s | %s",
                "S".equals(result) ? "SUCCESS" : "FAILURE",
                category, action, actor,
                targetId != null ? targetId : "-",
                detail   != null ? detail   : "-");
        // ※ IP 주소는 로그에 포함하지 않음 — DB 저장(암호화)으로만 기록

        if ("S".equals(result)) log.info(msg);
        else                    log.warn(msg);

        // IP, URI 자동 수집 — DB 저장 전용
        String clientIp   = resolveClientIp();
        String requestUri = resolveRequestUri();

        try {
            if (AUTH.equals(category)) {
                Long   actorId   = adminId != null ? adminId : userId;
                String actorType = adminId != null ? "ADMIN" : "USER";
                auditLogMapper.insertSecurityLog(
                        actorType, actorId, action,
                        null, null, detail, clientIp);

            } else if (isAdminCategory(category)) {
                auditLogMapper.insertAdminActivity(
                        AdminActivityLog.builder()
                                .adminId(adminId)
                                .action(category + "_" + action)
                                .result(result)
                                .targetId(targetId)
                                .detail(detail)
                                .clientIp(clientIp)    // DB에는 암호화 저장
                                .requestUri(requestUri)
                                .build());
            } else {
                auditLogMapper.insertUserActivity(
                        UserActivityLog.builder()
                                .userId(userId)
                                .action(category + "_" + action)
                                .result(result)
                                .targetId(targetId)
                                .detail(detail)
                                .clientIp(clientIp)    // DB에는 암호화 저장
                                .requestUri(requestUri)
                                .build());
            }
        } catch (Exception e) {
            log.error("[AUDIT] DB 저장 실패 — category={} action={} result={}", category, action, result, e);
        }
    }

    /**
     * USER, WATCHLIST 카테고리 포함.
     * - USER     : 관리자에 의한 회원 관리 → ADMIN_ACTIVITY_LOG
     * - WATCHLIST: 요주의 인물 등록/삭제  → ADMIN_ACTIVITY_LOG
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
     * 클라이언트 IP 추출 — DB 저장 전용, Slf4j 로그에는 기록하지 않음.
     *
     * server.forward-headers-strategy=framework 설정으로
     * Spring이 신뢰 프록시의 XFF만 적용하여 RemoteAddr를 실제 클라이언트 IP로 교체.
     * XFF 헤더를 직접 읽지 않으므로 IP 스푸핑 방어됨.
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