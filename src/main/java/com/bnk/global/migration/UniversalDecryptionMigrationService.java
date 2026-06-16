package com.bnk.global.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 전 테이블 범용 AES 복호화 마이그레이션 서비스.
 *
 * [동작 방식]
 *  1. Oracle USER_TAB_COLUMNS에서 모든 VARCHAR2 컬럼 목록을 런타임에 조회
 *  2. 각 컬럼에 대해 REGEXP_COUNT(컬럼, ':') = 1 인 행(암호문 후보)을 배치로 읽음
 *  3. decryptDirect()로 복호화 시도
 *     - Base64 실패 / GCM 인증 실패 → null → skip (콜론 포함 평문)
 *     - 복호화 결과 == 원문 → skip (암호문 아님)
 *     - 복호화 성공 + 원문과 다름 → UPDATE
 *
 * [로그 테이블]
 *  UPDATE 차단 트리거가 있는 테이블은 트리거를 런타임에 DB 조회 후 DISABLE/ENABLE.
 *  LOGIN_HISTORIES는 트리거 없음 → 일반 UPDATE.
 *
 * [보안]
 *  - tableName / columnName / pkColumn 은 Oracle 식별자이므로 PreparedStatement ? 바인딩 불가.
 *    Oracle 메타데이터(USER_TAB_COLUMNS)에서 온 값이라도 방어 심층(Defense in Depth) 차원에서
 *    VALID_IDENTIFIER 정규식으로 반드시 추가 검증 후 SQL에 삽입한다.
 *  - 실제 데이터 값(plainText, pkValue, offset, limit 등)은
 *    PreparedStatement의 setXxx() 파라미터 바인딩으로 처리한다.
 *
 * [트랜잭션]
 *  migrateAll()  : @Transactional — 외부 트랜잭션 컨텍스트 제공
 *  processColumn(): @Transactional(MANDATORY) — 반드시 기존 트랜잭션 안에서만 호출
 *  (기존: 주석 처리된 REQUIRES_NEW → 호출자와 Propagation 불일치 상태였음)
 *
 * [멱등성]
 *  복호화된 평문은 REGEXP_COUNT(':')=1 조건 불충족 → 재실행 시 자동 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniversalDecryptionMigrationService {

    private final UniversalDecryptionMigrationMapper mapper;
    private final AesCryptoUtil                      aesCryptoUtil;
    private final DataSource                         dataSource;

    private static final int BATCH_SIZE = 200;

    /**
     * [보안] Oracle 식별자 유효성 검증 정규식.
     *
     * Oracle 명명 규칙: 영문 대문자로 시작, 영문·숫자·언더스코어 조합, 최대 30자.
     * USER_TAB_COLUMNS에서 온 메타데이터도 이 패턴을 반드시 만족해야 한다.
     * 패턴 불일치 시 즉시 차단 → SQL Injection 방어 2중 레이어.
     */
    private static final Pattern VALID_IDENTIFIER =
        Pattern.compile("^[A-Z][A-Z0-9_]{0,29}$");

    /**
     * UPDATE 차단 트리거가 존재하는 로그 테이블.
     * 트리거명은 getExistingTriggerName()에서 DB 실존 여부 확인 후 사용.
     * LOGIN_HISTORIES 는 트리거 없음 → 제외.
     */
    private static final Set<String> LOG_TABLES = new HashSet<>(Arrays.asList(
        "AUDIT_LOGS",
        "ADMIN_ACTIVITY_LOG",
        "USER_ACTIVITY_LOG"
    ));

    public record ColumnResult(String table, String column, int success, int fail, int skip) {}
    public record MigrationResult(int totalSuccess, int totalFail, int totalSkip,
                                  int columnsProcessed, int columnsWithData) {}

    // ══════════════════════════════════════════════════════════════
    // 전체 실행
    // [수정] @Transactional 추가 — processColumn(MANDATORY)의 호출자로서
    //        트랜잭션 컨텍스트를 반드시 제공해야 함 (Propagation 정합성 확보)
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public MigrationResult migrateAll() {
        log.info("[UniversalMigration] ===== 전 테이블 AES 복호화 스캔 시작 =====");

        List<Map<String, String>> columns = mapper.findAllVarcharColumns();
        log.info("[UniversalMigration] 스캔 대상 컬럼: {}개", columns.size());

        int totalSuccess = 0, totalFail = 0, totalSkip = 0;
        int columnsProcessed = 0, columnsWithData = 0;

        for (Map<String, String> col : columns) {
            String tableName  = col.get("TABLE_NAME");
            String columnName = col.get("COLUMN_NAME");
            String pkColumn   = col.get("PK_COLUMN");

            columnsProcessed++;
            try {
                ColumnResult result = processColumn(tableName, columnName, pkColumn);
                totalSuccess += result.success();
                totalFail    += result.fail();
                totalSkip    += result.skip();
                if (result.success() + result.fail() > 0) columnsWithData++;
            } catch (Exception e) {
                log.error("[UniversalMigration] {}.{} 치명 예외: {}",
                        tableName, columnName, e.getMessage(), e);
            }
        }

        log.info("[UniversalMigration] ===== 완료 ===== 처리컬럼={}, 데이터컬럼={}, 성공={}, 실패={}, skip={}",
                columnsProcessed, columnsWithData, totalSuccess, totalFail, totalSkip);

        return new MigrationResult(totalSuccess, totalFail, totalSkip, columnsProcessed, columnsWithData);
    }

    // ══════════════════════════════════════════════════════════════
    // 컬럼 단위 처리
    // [수정] @Transactional(MANDATORY) 적용
    //   - migrateAll()의 @Transactional 컨텍스트 안에서만 호출 허용
    //   - 트랜잭션 없이 직접 호출 시 IllegalTransactionStateException → 오용 방지
    //   - 기존: 주석 처리된 REQUIRES_NEW 상태 (호출자 비트랜잭션 → Propagation 불일치)
    // ══════════════════════════════════════════════════════════════
    @Transactional(propagation = Propagation.MANDATORY)
    public ColumnResult processColumn(String tableName, String columnName, String pkColumn) {

        // [보안] Step 1 — 식별자 정규식 검증: 메타데이터 값이라도 반드시 통과해야 함
        validateIdentifier(tableName);
        validateIdentifier(columnName);
        validateIdentifier(pkColumn);

        int total;
        try {
            total = mapper.countEncryptedRows(tableName, columnName, pkColumn);
        } catch (Exception e) {
            log.error("[UniversalMigration] {}.{} COUNT 실패: {}", tableName, columnName, e.getMessage());
            return new ColumnResult(tableName, columnName, 0, 0, 0);
        }

        if (total == 0) return new ColumnResult(tableName, columnName, 0, 0, 0);

        log.info("[UniversalMigration] {}.{} — 후보 {}건 복호화 시작", tableName, columnName, total);

        boolean isLogTable = LOG_TABLES.contains(tableName);
        int success = 0, fail = 0, skip = 0, offset = 0;

        while (offset < total) {
            List<Map<String, Object>> batch;
            try {
                batch = mapper.findEncryptedRows(tableName, columnName, pkColumn, BATCH_SIZE, offset);
            } catch (Exception e) {
                log.error("[UniversalMigration] {}.{} SELECT 실패 offset={}: {}",
                        tableName, columnName, offset, e.getMessage());
                break;
            }

            if (batch.isEmpty()) break;

            for (Map<String, Object> row : batch) {
                Object pkValue  = row.get("PK_VALUE");
                String rawValue = (String) row.get("COL_VALUE");

                if (rawValue == null || rawValue.isBlank()) { skip++; continue; }

                String plainText = tryDecrypt(rawValue);

                if (plainText == null)          { skip++; continue; }
                if (plainText.equals(rawValue)) { skip++; continue; }

                try {
                    if (isLogTable) {
                        updateWithTriggerControl(tableName, columnName, pkColumn, pkValue, plainText);
                    } else {
                        // plainText → pstmt.setString(1, plainText)
                        // pkValue   → pstmt.setObject(2, pkValue)
                        updateNormal(tableName, columnName, pkColumn, pkValue, plainText);
                    }
                    success++;
                } catch (Exception e) {
                    log.warn("[UniversalMigration] {}.{} pk={} — UPDATE 실패: {}",
                            tableName, columnName, pkValue, e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
        }

        log.info("[UniversalMigration] {}.{} 완료 — 성공:{}, 실패:{}, skip:{}",
                tableName, columnName, success, fail, skip);

        return new ColumnResult(tableName, columnName, success, fail, skip);
    }

    // ══════════════════════════════════════════════════════════════
    // [보안] 식별자 유효성 검증
    // ══════════════════════════════════════════════════════════════

    /**
     * Oracle 식별자(테이블명/컬럼명/PK컬럼명) 유효성 검증.
     * VALID_IDENTIFIER 정규식에 맞지 않는 값은 IllegalArgumentException으로 즉시 차단.
     */
    private void validateIdentifier(String name) {
        if (name == null || !VALID_IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "[UniversalMigration] 유효하지 않은 식별자 차단: " + name);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 일반 테이블 UPDATE
    //
    // [수정 전] jdbcTemplate.update(
    //               "UPDATE " + tableName + " SET " + columnName + " = ?"
    //               + " WHERE " + pkColumn + " = ?", plainText, pkValue);
    //           → 문자열 결합 → SQL Injection 취약
    //
    // [수정 후] validateIdentifier() 통과된 식별자 삽입
    //           + plainText → pstmt.setString(1, plainText)  — Good; PreparedStatement escapes inputs
    //           + pkValue   → pstmt.setObject(2, pkValue)    — Good; PreparedStatement escapes inputs
    // ══════════════════════════════════════════════════════════════
    private void updateNormal(String tableName, String columnName,
                              String pkColumn, Object pkValue, String plainText) {

        // 식별자: validateIdentifier() 통과 완료 — SQL Injection 차단됨
        // Oracle 식별자(테이블명/컬럼명)는 PreparedStatement ? 바인딩 대상이 아님
        // → VALID_IDENTIFIER 정규식(^[A-Z][A-Z0-9_]{0,29}$) + USER_TAB_COLUMNS 메타데이터 출처로 안전
        // 값: pstmt.setXxx() 파라미터 바인딩 적용
        String query = "UPDATE " + tableName // NOSONAR: tableName/columnName/pkColumn은 validateIdentifier() 정규식 검증 완료
                     + " SET " + columnName + " = ?"
                     + " WHERE " + pkColumn + " = ?";

        Connection        con   = null;
        PreparedStatement pstmt = null;

        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, plainText); // Good; PreparedStatement escapes inputs
            pstmt.setObject(2, pkValue);   // Good; PreparedStatement escapes inputs
            pstmt.executeUpdate();

        } catch (SQLException e) {
            log.error("[UniversalMigration] updateNormal 실패 {}.{} pk={}: {}",
                    tableName, columnName, pkValue, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 로그 테이블 UPDATE — 트리거 DISABLE/ENABLE 포함
    //
    // [수정 전] jdbcTemplate.update(
    //               "UPDATE " + tableName + " SET " + columnName + " = ?"
    //               + " WHERE " + pkColumn + " = ?", plainText, pkValue);
    //           → 동일한 SQL Injection 취약점
    //
    // [수정 후] validateIdentifier() 통과된 식별자 삽입
    //           + plainText → pstmt.setString(1, plainText)  — Good; PreparedStatement escapes inputs
    //           + pkValue   → pstmt.setObject(2, pkValue)    — Good; PreparedStatement escapes inputs
    // ══════════════════════════════════════════════════════════════
    private void updateWithTriggerControl(String tableName, String columnName,
                                          String pkColumn, Object pkValue, String plainText) {

        // 식별자 재검증 (독립 진입 경로 대비 방어 심층)
        validateIdentifier(tableName);
        validateIdentifier(columnName);
        validateIdentifier(pkColumn);

        String triggerName = getExistingTriggerName(tableName);

        // 트리거 DISABLE
        if (triggerName != null) {
            executeDdl("ALTER TRIGGER " + triggerName + " DISABLE");
        }

        Connection        con   = null;
        PreparedStatement pstmt = null;

        try {
            // 식별자: validateIdentifier() 통과 완료 — SQL Injection 차단됨
            // Oracle 식별자(테이블명/컬럼명)는 PreparedStatement ? 바인딩 대상이 아님
            // → VALID_IDENTIFIER 정규식(^[A-Z][A-Z0-9_]{0,29}$) + USER_TAB_COLUMNS 메타데이터 출처로 안전
            String query = "UPDATE " + tableName // NOSONAR: tableName/columnName/pkColumn은 validateIdentifier() 정규식 검증 완료
                         + " SET " + columnName + " = ?"
                         + " WHERE " + pkColumn + " = ?";

            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, plainText); // Good; PreparedStatement escapes inputs
            pstmt.setObject(2, pkValue);   // Good; PreparedStatement escapes inputs
            pstmt.executeUpdate();

        } catch (SQLException e) {
            log.error("[UniversalMigration] updateWithTriggerControl 실패 {}.{} pk={}: {}",
                    tableName, columnName, pkValue, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            closeQuietly(null, pstmt, con);
            // 트리거 ENABLE — 성공/실패 무관하게 반드시 수행
            if (triggerName != null) {
                try {
                    executeDdl("ALTER TRIGGER " + triggerName + " ENABLE");
                } catch (Exception e) {
                    log.error("[UniversalMigration] 트리거 재활성화 실패: {}", triggerName, e);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 복호화 시도
    // ══════════════════════════════════════════════════════════════
    private String tryDecrypt(String rawValue) {
        try {
            return aesCryptoUtil.decryptDirect(rawValue);
        } catch (Exception e) {
            // Base64 디코딩 실패 / GCM 인증 실패 / 콜론 2개 이상 → null 반환 → skip
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 트리거 실존 확인
    //
    // [보안] candidate 는 switch 내부 하드코딩 상수만 사용 — 외부 입력 없음.
    //        DB 조회 시 candidate 값은 pstmt.setString()으로 바인딩.
    // ══════════════════════════════════════════════════════════════
    private String getExistingTriggerName(String tableName) {
        String candidate = switch (tableName) {
            case "AUDIT_LOGS"         -> "TRG_AUDIT_LOGS_NO_UPD";
            case "ADMIN_ACTIVITY_LOG" -> "TRG_ADMIN_ACTIVITY_LOG_NO_UPD";
            case "USER_ACTIVITY_LOG"  -> "TRG_USER_ACTIVITY_LOG_NO_UPD";
            default -> null;
        };

        if (candidate == null) return null;

        String query = "SELECT COUNT(1) FROM USER_TRIGGERS WHERE TRIGGER_NAME = ?";

        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;

        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, candidate); // Good; PreparedStatement escapes inputs
            rs    = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return candidate;
            return null;

        } catch (SQLException e) {
            log.warn("[UniversalMigration] 트리거 실존 확인 실패 ({}): {}", candidate, e.getMessage());
            return null;
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DDL 실행 헬퍼 (트리거 DISABLE/ENABLE)
    // ══════════════════════════════════════════════════════════════
    private void executeDdl(String ddl) {
        Connection        con   = null;
        PreparedStatement pstmt = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(ddl);
            pstmt.execute();
        } catch (SQLException e) {
            log.warn("[UniversalMigration] DDL 실패: {} — {}", ddl, e.getMessage());
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 리소스 정리 헬퍼
    // ══════════════════════════════════════════════════════════════
    private void closeQuietly(ResultSet rs, PreparedStatement pstmt, Connection con) {
        if (rs    != null) try { rs.close();    } catch (SQLException ignored) {}
        if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
        if (con   != null) try { con.close();   } catch (SQLException ignored) {}
    }
}