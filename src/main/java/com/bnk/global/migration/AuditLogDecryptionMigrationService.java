package com.bnk.global.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

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
 * [보안]
 *  - 컬럼명(식별자)은 ALLOWED_COLUMNS 화이트리스트로 검증 후 SQL에 삽입.
 *    Oracle 식별자는 PreparedStatement의 ? 바인딩 대상이 아니므로
 *    화이트리스트 검증이 유일한 올바른 방어책이다.
 *  - 실제 데이터 값(plainText, id, offset, batchSize 등)은
 *    PreparedStatement의 setXxx() 파라미터 바인딩으로 처리한다.
 *
 * [멱등성]
 *  복호화된 평문은 isEncrypted() = false → 재실행 시 자동 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogDecryptionMigrationService {

    private final DataSource    dataSource;
    private final AesCryptoUtil aesCryptoUtil;

    private static final int    BATCH_SIZE   = 200;
    private static final String TRIGGER_NAME = "TRG_AUDIT_LOGS_NO_UPD";

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

    /**
     * [보안] SQL Injection 방지 — 컬럼명 화이트리스트.
     *
     * Oracle의 테이블명·컬럼명(식별자)은 PreparedStatement의 ? 바인딩 대상이 아니다.
     * JDBC 드라이버는 ? 위치에 리터럴 값만 바인딩하며, 식별자로 해석하지 않는다.
     * 따라서 식별자에 대한 유일한 방어책은 허용 목록(whitelist) 검증이다.
     *
     * TARGET_COLUMNS 배열과 반드시 동기화하여 관리한다.
     */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
        "ACTOR_TYPE_CODE",
        "ACTION_TYPE_CODE",
        "TARGET_TYPE_CODE",
        "DESCRIPTION",
        "USER_AGENT"
    );

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

        // [보안] Step 1 — 화이트리스트 검증: ALLOWED_COLUMNS에 없는 컬럼명 즉시 차단
        validateColumnName(columnName);

        // 1. 암호문 패턴 행 수 조회
        int cnt = countEncryptedRows(columnName);
        log.info("[AuditLogDecryption] AUDIT_LOGS.{} 후보 {}건", columnName, cnt);

        if (cnt == 0) return new ColumnResult(columnName, 0, 0, 0);

        int success = 0, fail = 0, skip = 0, offset = 0;

        while (offset < cnt) {
            // 2. 배치 조회 — offset, BATCH_SIZE는 pstmt.setInt() 바인딩
            List<Row> batch = fetchBatch(columnName, offset, BATCH_SIZE);

            if (batch.isEmpty()) break;

            for (Row row : batch) {
                String rawValue = row.colValue();

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

                    // plainText → pstmt.setString(1, plainText)
                    // id        → pstmt.setObject(2, id)
                    updateDecrypted(columnName, plainText, row.auditLogId());

                    log.debug("[AuditLogDecryption] AUDIT_LOGS.{} id={} 복호화: [{}] -> [{}]",
                            columnName, row.auditLogId(),
                            rawValue.substring(0, Math.min(20, rawValue.length())),
                            plainText);
                    success++;

                } catch (Exception e) {
                    log.warn("[AuditLogDecryption] AUDIT_LOGS.{} id={} 복호화 실패: {}",
                            columnName, row.auditLogId(), e.getMessage());
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
    // [보안] 화이트리스트 검증
    // ==================================================================

    /**
     * 컬럼명 화이트리스트 검증.
     * ALLOWED_COLUMNS에 없는 값은 IllegalArgumentException으로 즉시 차단한다.
     */
    private void validateColumnName(String columnName) {
        if (columnName == null || !ALLOWED_COLUMNS.contains(columnName)) {
            throw new IllegalArgumentException(
                "[AuditLogDecryption] 허용되지 않은 컬럼명 차단: " + columnName);
        }
    }

    // ==================================================================
    // COUNT — PreparedStatement, 바인딩 파라미터 없음
    // ==================================================================

    /**
     * 암호문 패턴(콜론 정확히 1개) 행 수 조회.
     *
     * columnName은 validateColumnName() 통과 완료된 식별자 → SQL에 안전하게 삽입.
     * 조건 값이 없으므로 추가 setXxx() 호출 불필요.
     */
    private int countEncryptedRows(String columnName) {

        // columnName은 validateColumnName() 통과 완료 — SQL Injection 차단됨
        // ALLOWED_COLUMNS 화이트리스트 검증 완료: {ACTOR_TYPE_CODE, ACTION_TYPE_CODE, TARGET_TYPE_CODE, DESCRIPTION, USER_AGENT}
        // Oracle 식별자는 PreparedStatement ? 바인딩 대상이 아님 → 화이트리스트 검증이 유일한 방어책
        String query = "SELECT COUNT(1) FROM AUDIT_LOGS" // NOSONAR: columnName은 ALLOWED_COLUMNS 화이트리스트 검증 완료
                     + " WHERE " + columnName + " IS NOT NULL"
                     + " AND REGEXP_COUNT(" + columnName + ", ':') = 1";

        Connection   con   = null;
        PreparedStatement pstmt = null;
        ResultSet    rs    = null;

        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query); // 조건 값 없음 — setXxx() 불필요
            rs    = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            log.error("[AuditLogDecryption] COUNT 실패 컬럼={}: {}", columnName, e.getMessage());
            return 0;
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    // ==================================================================
    // 배치 SELECT — offset/batchSize → pstmt.setInt() 바인딩
    // ==================================================================

    /**
     * 암호문 패턴 행 배치 조회.
     *
     * columnName : validateColumnName() 통과 완료된 식별자
     * offset     : pstmt.setInt(1, offset)    — Good; PreparedStatement escapes inputs
     * batchSize  : pstmt.setInt(2, batchSize) — Good; PreparedStatement escapes inputs
     */
    private List<Row> fetchBatch(String columnName, int offset, int batchSize) {

        // columnName은 validateColumnName() 통과 완료 — SQL Injection 차단됨
        // Oracle 식별자는 PreparedStatement ? 바인딩 대상이 아님 → 화이트리스트 검증이 유일한 방어책
        // offset, batchSize는 외부 유사값 → pstmt.setInt()로 바인딩
        String query = "SELECT AUDIT_LOG_ID, " + columnName + " AS COL_VALUE" // NOSONAR: columnName은 ALLOWED_COLUMNS 화이트리스트 검증 완료
                     + " FROM AUDIT_LOGS"
                     + " WHERE " + columnName + " IS NOT NULL"
                     + " AND REGEXP_COUNT(" + columnName + ", ':') = 1"
                     + " ORDER BY AUDIT_LOG_ID"
                     + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;

        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setInt(1, offset);    // Good; PreparedStatement escapes inputs
            pstmt.setInt(2, batchSize); // Good; PreparedStatement escapes inputs
            rs    = pstmt.executeQuery();

            List<Row> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new Row(rs.getObject("AUDIT_LOG_ID"), rs.getString("COL_VALUE")));
            }
            return rows;

        } catch (SQLException e) {
            log.error("[AuditLogDecryption] SELECT 실패 컬럼={} offset={}: {}", columnName, offset, e.getMessage());
            return List.of();
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    // ==================================================================
    // UPDATE — plainText/id → pstmt.setString()/setObject() 바인딩
    // ==================================================================

    /**
     * 복호화된 값으로 단건 UPDATE.
     *
     * columnName : validateColumnName() 통과 완료된 식별자
     * plainText  : pstmt.setString(1, plainText) — Good; PreparedStatement escapes inputs
     * id         : pstmt.setObject(2, id)         — Good; PreparedStatement escapes inputs
     */
    private void updateDecrypted(String columnName, String plainText, Object id) {

        // columnName은 validateColumnName() 통과 완료 — SQL Injection 차단됨
        // Oracle 식별자는 PreparedStatement ? 바인딩 대상이 아님 → 화이트리스트 검증이 유일한 방어책
        // plainText, id는 외부 데이터 값 → pstmt.setXxx()로 바인딩
        String query = "UPDATE AUDIT_LOGS" // NOSONAR: columnName은 ALLOWED_COLUMNS 화이트리스트 검증 완료
                     + " SET " + columnName + " = ?"
                     + " WHERE AUDIT_LOG_ID = ?";

        Connection        con   = null;
        PreparedStatement pstmt = null;

        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, plainText); // Good; PreparedStatement escapes inputs
            pstmt.setObject(2, id);        // Good; PreparedStatement escapes inputs
            pstmt.executeUpdate();

        } catch (SQLException e) {
            log.error("[AuditLogDecryption] UPDATE 실패 컬럼={} id={}: {}", columnName, id, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    // ==================================================================
    // 배치 조회 결과 레코드
    // ==================================================================
    private record Row(Object auditLogId, String colValue) {}

    // ==================================================================
    // 트리거 DISABLE / ENABLE
    // ==================================================================
    private void disableTrigger() {
        Connection        con   = null;
        PreparedStatement pstmt = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement("ALTER TRIGGER " + TRIGGER_NAME + " DISABLE");
            pstmt.execute();
            log.info("[AuditLogDecryption] 트리거 DISABLE: {}", TRIGGER_NAME);
        } catch (SQLException e) {
            log.warn("[AuditLogDecryption] 트리거 DISABLE 실패 (존재하지 않을 수 있음): {} — {}",
                    TRIGGER_NAME, e.getMessage());
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    private void enableTrigger() {
        Connection        con   = null;
        PreparedStatement pstmt = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement("ALTER TRIGGER " + TRIGGER_NAME + " ENABLE");
            pstmt.execute();
            log.info("[AuditLogDecryption] 트리거 ENABLE: {}", TRIGGER_NAME);
        } catch (SQLException e) {
            log.error("[AuditLogDecryption] 트리거 ENABLE 실패 — 수동으로 활성화 필요: ALTER TRIGGER {} ENABLE",
                    TRIGGER_NAME, e);
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    // ==================================================================
    // 리소스 정리 헬퍼
    // ==================================================================
    private void closeQuietly(ResultSet rs, PreparedStatement pstmt, Connection con) {
        if (rs    != null) try { rs.close();    } catch (SQLException ignored) {}
        if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
        if (con   != null) try { con.close();   } catch (SQLException ignored) {}
    }
}