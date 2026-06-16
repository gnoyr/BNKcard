package com.bnk.global.migration;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AUDIT_LOGS 잘못 암호화된 컬럼 복호화 전용 마이그레이션 서비스.
 *
 * [배경]
 *  UniversalDecryptionMigrationService Phase 2 스캔이
 *  AUDIT_LOGS 컬럼들을 암호화 대상으로 오분류하여
 *  평문값을 AES 암호화해버린 상황을 복구한다.
 *
 * [복구 대상 컬럼 — IP_ADDRESS 제외 전부 평문 복구]
 *  - ACTOR_TYPE_CODE  : "AUTH", "ADMIN", "SYSTEM" 등
 *  - ACTION_TYPE_CODE : "LOGIN", "SIGNUP", "EMAIL_VERIFY" 등
 *  - TARGET_TYPE_CODE : "USER", "ADMIN" 등
 *  - DESCRIPTION      : 한국어 설명 문자열
 *  - USER_AGENT       : HTTP User-Agent 문자열
 *
 * [암호화 유지 컬럼]
 *  - IP_ADDRESS : 개인정보 보호 대상 — 암호화 상태 유지
 *
 * [트리거 처리]
 *  AUDIT_LOGS 테이블에 TRG_AUDIT_LOGS_NO_UPD 트리거가 존재하여 UPDATE를 차단함.
 *  UPDATE 전 DISABLE → UPDATE → ENABLE 순서로 처리.
 *  ENABLE은 finally에서 보장.
 *
 * [멱등성]
 *  복호화된 평문은 isEncrypted() = false → 재실행 시 자동 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogDecryptionMigrationService {

    private final JdbcTemplate  jdbcTemplate;
    private final AesCryptoUtil aesCryptoUtil;

    private static final int    BATCH_SIZE      = 200;
    private static final String TRIGGER_NAME    = "TRG_AUDIT_LOGS_NO_UPD";

    /**
     * 복구 대상 컬럼 목록.
     * IP_ADDRESS만 제외 (개인정보 보호 대상 — 암호화 유지).
     */
    private static final String[] TARGET_COLUMNS = {
        "ACTOR_TYPE_CODE",
        "ACTION_TYPE_CODE",
        "TARGET_TYPE_CODE",
        "DESCRIPTION",
        "USER_AGENT"
    };

    public record ColumnResult(String column, int success, int fail, int skip) {}
    public record MigrationResult(int totalSuccess, int totalFail, int totalSkip) {}

    // ==================================================================
    // 전체 실행
    // ==================================================================
    public MigrationResult migrateAll() {
        log.info("[AuditLogDecryption] ===== AUDIT_LOGS 복호화 시작 =====");

        // 트리거 한 번만 DISABLE → 전 컬럼 처리 → ENABLE
        disableTrigger();
        try {
            int totalSuccess = 0, totalFail = 0, totalSkip = 0;

            for (String column : TARGET_COLUMNS) {
                try {
                    ColumnResult result = decryptColumn(column);
                    totalSuccess += result.success();
                    totalFail    += result.fail();
                    totalSkip    += result.skip();
                } catch (Exception e) {
                    log.error("[AuditLogDecryption] {} 처리 중 치명 예외: {}", column, e.getMessage(), e);
                }
            }

            log.info("[AuditLogDecryption] ===== 완료 ===== 성공:{}, 실패:{}, skip:{}",
                    totalSuccess, totalFail, totalSkip);

            return new MigrationResult(totalSuccess, totalFail, totalSkip);

        } finally {
            enableTrigger();
        }
    }

    // ==================================================================
    // 컬럼 단위 복호화 (트리거는 migrateAll에서 제어)
    // ==================================================================
    public ColumnResult decryptColumn(String columnName) {

        // 1. 암호문 패턴 행 수 조회
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM AUDIT_LOGS " +
            "WHERE " + columnName + " IS NOT NULL " +
            "AND REGEXP_COUNT(" + columnName + ", ':') = 1",
            Integer.class);

        int cnt = (total == null) ? 0 : total;
        log.info("[AuditLogDecryption] AUDIT_LOGS.{} 후보 {}건", columnName, cnt);

        if (cnt == 0) return new ColumnResult(columnName, 0, 0, 0);

        int success = 0, fail = 0, skip = 0, offset = 0;

        while (offset < cnt) {
            // 2. 배치 조회
            List<Map<String, Object>> batch = jdbcTemplate.queryForList(
                "SELECT AUDIT_LOG_ID, " + columnName + " AS COL_VALUE " +
                "FROM AUDIT_LOGS " +
                "WHERE " + columnName + " IS NOT NULL " +
                "AND REGEXP_COUNT(" + columnName + ", ':') = 1 " +
                "ORDER BY AUDIT_LOG_ID " +
                "OFFSET " + offset + " ROWS FETCH NEXT " + BATCH_SIZE + " ROWS ONLY");

            if (batch.isEmpty()) break;

            for (Map<String, Object> row : batch) {
                Object id       = row.get("AUDIT_LOG_ID");
                String rawValue = (String) row.get("COL_VALUE");

                if (rawValue == null || rawValue.isBlank()) {
                    skip++;
                    continue;
                }

                // 3. AES 암호문인지 엄격 검증
                if (!aesCryptoUtil.isEncrypted(rawValue)) {
                    skip++;
                    continue;
                }

                // 4. 복호화 후 UPDATE
                try {
                    String plainText = aesCryptoUtil.decryptDirect(rawValue);

                    if (plainText == null || plainText.equals(rawValue)) {
                        skip++;
                        continue;
                    }

                    jdbcTemplate.update(
                        "UPDATE AUDIT_LOGS SET " + columnName + " = ? WHERE AUDIT_LOG_ID = ?",
                        plainText, id);

                    log.debug("[AuditLogDecryption] AUDIT_LOGS.{} id={} 복호화: [{}] -> [{}]",
                            columnName, id,
                            rawValue.substring(0, Math.min(20, rawValue.length())),
                            plainText);
                    success++;

                } catch (Exception e) {
                    log.warn("[AuditLogDecryption] AUDIT_LOGS.{} id={} 복호화 실패: {}",
                            columnName, id, e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
        }

        log.info("[AuditLogDecryption] AUDIT_LOGS.{} 완료 — 성공:{}, 실패:{}, skip:{}",
                columnName, success, fail, skip);

        return new ColumnResult(columnName, success, fail, skip);
    }

    // ==================================================================
    // 트리거 DISABLE / ENABLE
    // ==================================================================
    private void disableTrigger() {
        try {
            jdbcTemplate.execute("ALTER TRIGGER " + TRIGGER_NAME + " DISABLE");
            log.info("[AuditLogDecryption] 트리거 DISABLE: {}", TRIGGER_NAME);
        } catch (Exception e) {
            log.warn("[AuditLogDecryption] 트리거 DISABLE 실패 (존재하지 않을 수 있음): {} — {}",
                    TRIGGER_NAME, e.getMessage());
        }
    }

    private void enableTrigger() {
        try {
            jdbcTemplate.execute("ALTER TRIGGER " + TRIGGER_NAME + " ENABLE");
            log.info("[AuditLogDecryption] 트리거 ENABLE: {}", TRIGGER_NAME);
        } catch (Exception e) {
            log.error("[AuditLogDecryption] 트리거 ENABLE 실패 — 수동으로 활성화 필요: ALTER TRIGGER {} ENABLE",
                    TRIGGER_NAME, e);
        }
    }
}