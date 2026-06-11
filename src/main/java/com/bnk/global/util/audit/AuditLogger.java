package com.bnk.global.util.audit;

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
 *   AUTH                          → AUDIT_LOGS         (기존 테이블)
 *   CARD_APPLY / TERMS / FILE     → USER_ACTIVITY_LOG  (신규)
 *   CARD / CDD / ADMIN / EXTERNAL_API → ADMIN_ACTIVITY_LOG (신규)
 *
 * ── 기존 코드와의 관계 ────────────────────────────────────────────────
 *   UserMapper.insertAuditLog()      기존 유지 (AUDIT_LOGS 직접 INSERT)
 *   AdminUserMapper.insertAuditLog() 기존 유지 (AUDIT_LOGS 직접 INSERT)
 *   → 신규 코드부터 AuditLogger 사용으로 점진 전환
 *
 * ── 로그 포맷 예시 ────────────────────────────────────────────────────
 *   [AUDIT] SUCCESS | CARD_APPLY | APPLY            | userId=3  | target=101 | -
 *   [AUDIT] FAILURE | AUTH       | LOGIN            | userId=-  | target=-   | 비밀번호 불일치
 *   [AUDIT] SUCCESS | CARD       | APPROVAL_APPROVE | adminId=2 | target=5   | -
 *
 * ── 사용법 ───────────────────────────────────────────────────────────
 *   private final AuditLogger auditLogger;  // @RequiredArgsConstructor
 *
 *   auditLogger.success(AuditLogger.AUTH, AuditLogger.LOGIN, userId, null, null);
 *   auditLogger.failure(AuditLogger.AUTH, AuditLogger.LOGIN, null, null, "비밀번호 불일치");
 *   auditLogger.success(AuditLogger.CARD_APPLY, AuditLogger.APPLY, userId, String.valueOf(cardId), null);
 *   auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE, adminId, String.valueOf(approvalId), null);
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

	private final AuditLogMapper auditLogMapper;

	// ── 카테고리 상수 ─────────────────────────────────────────────────

	/** 보안 이벤트 → AUDIT_LOGS (기존 테이블) */
	public static final String AUTH = "AUTH";

	/** 사용자 행위 → USER_ACTIVITY_LOG */
	public static final String CARD_APPLY = "CARD_APPLY";
	public static final String TERMS = "TERMS";
	public static final String FILE = "FILE";

	/** 관리자 행위 → ADMIN_ACTIVITY_LOG */
	public static final String CARD = "CARD";
	public static final String CDD = "CDD";
	public static final String ADMIN = "ADMIN";
	public static final String EXTERNAL_API = "EXTERNAL_API";

	// ── 액션 상수 ─────────────────────────────────────────────────────
	public static final String LOGIN = "LOGIN";
	public static final String LOGOUT = "LOGOUT";
	public static final String SIGNUP = "SIGNUP";
	public static final String WITHDRAW = "WITHDRAW";
	public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
	public static final String EMAIL_VERIFY = "EMAIL_VERIFY";
	public static final String TOKEN_REFRESH = "TOKEN_REFRESH";
	public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

	public static final String APPLY = "APPLY";
	public static final String APPLY_CANCEL = "APPLY_CANCEL";
	public static final String AGREE = "AGREE";
	public static final String UPLOAD = "UPLOAD";
	public static final String DOWNLOAD = "DOWNLOAD";

	public static final String CREATE = "CREATE";
	public static final String UPDATE = "UPDATE";
	public static final String STATUS_CHANGE = "STATUS_CHANGE";
	public static final String APPROVAL_REQUEST = "APPROVAL_REQUEST";
	public static final String APPROVAL_APPROVE = "APPROVAL_APPROVE";
	public static final String APPROVAL_REJECT = "APPROVAL_REJECT";
	public static final String API_CALL = "API_CALL";
	public static final String CDD_STATUS_CHANGE = "CDD_STATUS_CHANGE";
	public static final String WATCHLIST_REGISTER = "WATCHLIST_REGISTER";
	public static final String WATCHLIST_REMOVE = "WATCHLIST_REMOVE";

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

        // ② DB INSERT — 실패해도 본 비즈니스 로직에 영향 없도록 try-catch
        try {
            if (AUTH.equals(category)) {
                // 보안 이벤트 → 기존 AUDIT_LOGS
                Long   actorId   = adminId != null ? adminId : userId;
                String actorType = adminId != null ? "ADMIN" : "USER";
                auditLogMapper.insertSecurityLog(
                        actorType, actorId, action,
                        null, null, detail, null);

            } else if (isAdminCategory(category)) {
                // 관리자 행위 → ADMIN_ACTIVITY_LOG
                auditLogMapper.insertAdminActivity(
                        AdminActivityLog.builder()
                                .adminId(adminId)
                                .action(category + "_" + action)
                                .result(result)
                                .targetId(targetId)
                                .detail(detail)
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
                                .build());
            }
        } catch (Exception e) {
            log.error("[AUDIT] DB 저장 실패 — category={} action={} result={}", category, action, result, e);
        }
    }

    private boolean isAdminCategory(String category) {
        return CARD.equals(category)
            || CDD.equals(category)
            || ADMIN.equals(category)
            || EXTERNAL_API.equals(category);
    }
}
