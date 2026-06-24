package com.bnk.global.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 암호화 돼야 하는데 평문으로 남아있는 컬럼 명시적 암호화 서비스.
 *
 * [대상]
 *  - EVENT_LOGS.REQUEST_IP      : IP 주소 → AES 암호화
 *  - LOGIN_HISTORIES.IP_ADDRESS : IP 주소 → AES 암호화
 *  - AUDIT_LOGS.IP_ADDRESS      : IP 주소 → AES 암호화 (트리거 DISABLE/ENABLE 필요)
 *
 * [보안]
 *  모든 쿼리는 테이블명·컬럼명이 하드코딩 상수이므로 식별자 인젝션 위험 없음.
 *  실제 데이터 값(val, id, offset, batchSize 등)은
 *  PreparedStatement의 setXxx() 파라미터 바인딩으로 처리한다.
 *
 * [멱등성]
 *  isEncrypted() 확인 후 이미 암호화된 행은 skip.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiscEncryptionMigrationService {

    private final DataSource    dataSource;
    private final AesCryptoUtil aesCryptoUtil;

    private static final int BATCH_SIZE = 200;

    public record MigrationResult(int successCount, int failCount) {}

    // ── 전체 실행 ──────────────────────────────────────────────────
    @Transactional
    public MigrationResult migrateAll() {
        MigrationResult r1 = migrateEventLogsRequestIp();
        MigrationResult r2 = migrateLoginHistoriesIpAddress();
        MigrationResult r3 = migrateAuditLogsIpAddress();
        return new MigrationResult(
                r1.successCount() + r2.successCount() + r3.successCount(),
                r1.failCount()    + r2.failCount()    + r3.failCount());
    }

    // ══════════════════════════════════════════════════════════════
    // EVENT_LOGS.REQUEST_IP
    // ══════════════════════════════════════════════════════════════

    /**
     * EVENT_LOGS.REQUEST_IP 평문 → AES 암호화.
     *
     * 테이블·컬럼명 하드코딩 — 식별자 인젝션 위험 없음.
     * offset, batchSize → pstmt.setInt()     바인딩.
     * encrypted, id     → pstmt.setString()/setObject() 바인딩.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MigrationResult migrateEventLogsRequestIp() {

        int cnt = countPlainIp_EventLogs();
        log.info("[MiscEncryption] EVENT_LOGS.REQUEST_IP 평문 대상: {}건", cnt);
        if (cnt == 0) return new MigrationResult(0, 0);

        int success = 0, fail = 0, offset = 0;

        while (offset < cnt) {
            // offset, batchSize → pstmt.setInt() 바인딩
            List<IpRow> batch = fetchBatch_EventLogs(offset, BATCH_SIZE);
            if (batch.isEmpty()) break;

            for (IpRow row : batch) {
                try {
                    if (aesCryptoUtil.isEncrypted(row.ip())) { success++; continue; }
                    String encrypted = aesCryptoUtil.encrypt(row.ip());
                    // encrypted → pstmt.setString(1, encrypted)
                    // id        → pstmt.setObject(2, id)
                    update_EventLogs(row.id(), encrypted);
                    success++;
                } catch (Exception e) {
                    log.warn("[MiscEncryption] EVENT_LOGS.REQUEST_IP id={} 실패: {}", row.id(), e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
        }
        log.info("[MiscEncryption] EVENT_LOGS.REQUEST_IP 완료 — 성공:{}, 실패:{}", success, fail);
        return new MigrationResult(success, fail);
    }

    // ══════════════════════════════════════════════════════════════
    // LOGIN_HISTORIES.IP_ADDRESS
    // ══════════════════════════════════════════════════════════════

    /**
     * LOGIN_HISTORIES.IP_ADDRESS 평문 → AES 암호화.
     *
     * offset, batchSize → pstmt.setInt()     바인딩.
     * encrypted, id     → pstmt.setString()/setObject() 바인딩.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MigrationResult migrateLoginHistoriesIpAddress() {

        int cnt = countPlainIp_LoginHistories();
        log.info("[MiscEncryption] LOGIN_HISTORIES.IP_ADDRESS 평문 대상: {}건", cnt);
        if (cnt == 0) return new MigrationResult(0, 0);

        int success = 0, fail = 0, offset = 0;

        while (offset < cnt) {
            // offset, batchSize → pstmt.setInt() 바인딩
            List<IpRow> batch = fetchBatch_LoginHistories(offset, BATCH_SIZE);
            if (batch.isEmpty()) break;

            for (IpRow row : batch) {
                try {
                    if (aesCryptoUtil.isEncrypted(row.ip())) { success++; continue; }
                    String encrypted = aesCryptoUtil.encrypt(row.ip());
                    // encrypted → pstmt.setString(1, encrypted)
                    // id        → pstmt.setObject(2, id)
                    update_LoginHistories(row.id(), encrypted);
                    success++;
                } catch (Exception e) {
                    log.warn("[MiscEncryption] LOGIN_HISTORIES.IP_ADDRESS id={} 실패: {}", row.id(), e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
        }
        log.info("[MiscEncryption] LOGIN_HISTORIES.IP_ADDRESS 완료 — 성공:{}, 실패:{}", success, fail);
        return new MigrationResult(success, fail);
    }

    // ══════════════════════════════════════════════════════════════
    // AUDIT_LOGS.IP_ADDRESS — 트리거 DISABLE/ENABLE 필요
    // ══════════════════════════════════════════════════════════════

    /**
     * AUDIT_LOGS.IP_ADDRESS 평문 → AES 암호화.
     *
     * TRG_AUDIT_LOGS_NO_UPD 트리거가 UPDATE를 차단하므로
     * 작업 전 DISABLE, finally에서 반드시 ENABLE.
     *
     * offset, batchSize → pstmt.setInt()     바인딩.
     * encrypted, id     → pstmt.setString()/setObject() 바인딩.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MigrationResult migrateAuditLogsIpAddress() {

        // 1. 작업 전 트리거 비활성화 (트리거명 하드코딩 — 외부 입력 없음)
        executeDdl("ALTER TRIGGER ADMIN.TRG_AUDIT_LOGS_NO_UPD DISABLE");

        try {
            int cnt = countPlainIp_AuditLogs();
            log.info("[MiscEncryption] AUDIT_LOGS.IP_ADDRESS 평문 대상: {}건", cnt);
            if (cnt == 0) return new MigrationResult(0, 0);

            int success = 0, fail = 0, offset = 0;

            while (offset < cnt) {
                // offset, batchSize → pstmt.setInt() 바인딩
                List<IpRow> batch = fetchBatch_AuditLogs(offset, BATCH_SIZE);
                if (batch.isEmpty()) break;

                for (IpRow row : batch) {
                    try {
                        if (aesCryptoUtil.isEncrypted(row.ip())) { success++; continue; }
                        String encrypted = aesCryptoUtil.encrypt(row.ip());
                        // encrypted → pstmt.setString(1, encrypted)
                        // id        → pstmt.setObject(2, id)
                        update_AuditLogs(row.id(), encrypted);
                        success++;
                    } catch (Exception e) {
                        log.warn("[MiscEncryption] AUDIT_LOGS.IP_ADDRESS id={} 실패: {}", row.id(), e.getMessage());
                        fail++;
                    }
                }
                offset += batch.size();
            }
            log.info("[MiscEncryption] AUDIT_LOGS.IP_ADDRESS 완료 — 성공:{}, 실패:{}", success, fail);
            return new MigrationResult(success, fail);

        } finally {
            // 2. 성공/실패 무관하게 반드시 트리거 재활성화
            executeDdl("ALTER TRIGGER ADMIN.TRG_AUDIT_LOGS_NO_UPD ENABLE");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // PreparedStatement 헬퍼 — EVENT_LOGS
    // ══════════════════════════════════════════════════════════════

    /** COUNT — 하드코딩 SQL, 바인딩 파라미터 없음 */
    private int countPlainIp_EventLogs() {
        String query = "SELECT COUNT(1) FROM EVENT_LOGS"
                     + " WHERE REQUEST_IP IS NOT NULL"
                     + " AND REGEXP_COUNT(REQUEST_IP, ':') != 1";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            rs    = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("[MiscEncryption] EVENT_LOGS COUNT 실패: {}", e.getMessage());
            return 0;
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    /**
     * 배치 SELECT.
     */
    private List<IpRow> fetchBatch_EventLogs(int offset, int batchSize) {
        String query = "SELECT LOG_ID, REQUEST_IP FROM EVENT_LOGS"
                     + " WHERE REQUEST_IP IS NOT NULL"
                     + " AND REGEXP_COUNT(REQUEST_IP, ':') != 1"
                     + " ORDER BY LOG_ID"
                     + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setInt(1, offset);   
            pstmt.setInt(2, batchSize);
            rs    = pstmt.executeQuery();
            List<IpRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new IpRow(rs.getObject("LOG_ID"), rs.getString("REQUEST_IP")));
            }
            return rows;
        } catch (SQLException e) {
            log.error("[MiscEncryption] EVENT_LOGS SELECT 실패: {}", e.getMessage());
            return List.of();
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    /**
     * 단건 UPDATE.
     */
    private void update_EventLogs(Object id, String encrypted) {
        String query = "UPDATE EVENT_LOGS SET REQUEST_IP = ? WHERE LOG_ID = ?";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, encrypted);
            pstmt.setObject(2, id);      
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("[MiscEncryption] EVENT_LOGS UPDATE 실패 id={}: {}", id, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // PreparedStatement 헬퍼 — LOGIN_HISTORIES
    // ══════════════════════════════════════════════════════════════

    /** COUNT — 하드코딩 SQL, 바인딩 파라미터 없음 */
    private int countPlainIp_LoginHistories() {
        String query = "SELECT COUNT(1) FROM LOGIN_HISTORIES"
                     + " WHERE IP_ADDRESS IS NOT NULL"
                     + " AND REGEXP_COUNT(IP_ADDRESS, ':') != 1";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            rs    = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("[MiscEncryption] LOGIN_HISTORIES COUNT 실패: {}", e.getMessage());
            return 0;
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    /**
     * 배치 SELECT.
     * offset    → pstmt.setInt(1, offset)    — Good; PreparedStatement escapes inputs
     * batchSize → pstmt.setInt(2, batchSize) — Good; PreparedStatement escapes inputs
     */
    private List<IpRow> fetchBatch_LoginHistories(int offset, int batchSize) {
        String query = "SELECT HISTORY_ID, IP_ADDRESS FROM LOGIN_HISTORIES"
                     + " WHERE IP_ADDRESS IS NOT NULL"
                     + " AND REGEXP_COUNT(IP_ADDRESS, ':') != 1"
                     + " ORDER BY HISTORY_ID"
                     + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setInt(1, offset);  
            pstmt.setInt(2, batchSize);
            rs    = pstmt.executeQuery();
            List<IpRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new IpRow(rs.getObject("HISTORY_ID"), rs.getString("IP_ADDRESS")));
            }
            return rows;
        } catch (SQLException e) {
            log.error("[MiscEncryption] LOGIN_HISTORIES SELECT 실패: {}", e.getMessage());
            return List.of();
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    /**
     * 단건 UPDATE.
     */
    private void update_LoginHistories(Object id, String encrypted) {
        String query = "UPDATE LOGIN_HISTORIES SET IP_ADDRESS = ? WHERE HISTORY_ID = ?";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, encrypted); 
            pstmt.setObject(2, id);      
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("[MiscEncryption] LOGIN_HISTORIES UPDATE 실패 id={}: {}", id, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // PreparedStatement 헬퍼 — AUDIT_LOGS
    // ══════════════════════════════════════════════════════════════

    /** COUNT — 하드코딩 SQL, 바인딩 파라미터 없음 */
    private int countPlainIp_AuditLogs() {
        String query = "SELECT COUNT(1) FROM AUDIT_LOGS"
                     + " WHERE IP_ADDRESS IS NOT NULL"
                     + " AND REGEXP_COUNT(IP_ADDRESS, ':') != 1";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            rs    = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("[MiscEncryption] AUDIT_LOGS COUNT 실패: {}", e.getMessage());
            return 0;
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    /**
     * 배치 SELECT.
     */
    private List<IpRow> fetchBatch_AuditLogs(int offset, int batchSize) {
        String query = "SELECT AUDIT_LOG_ID, IP_ADDRESS FROM AUDIT_LOGS"
                     + " WHERE IP_ADDRESS IS NOT NULL"
                     + " AND REGEXP_COUNT(IP_ADDRESS, ':') != 1"
                     + " ORDER BY AUDIT_LOG_ID"
                     + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        ResultSet         rs    = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setInt(1, offset);    
            pstmt.setInt(2, batchSize); 
            rs    = pstmt.executeQuery();
            List<IpRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new IpRow(rs.getObject("AUDIT_LOG_ID"), rs.getString("IP_ADDRESS")));
            }
            return rows;
        } catch (SQLException e) {
            log.error("[MiscEncryption] AUDIT_LOGS SELECT 실패: {}", e.getMessage());
            return List.of();
        } finally {
            closeQuietly(rs, pstmt, con);
        }
    }

    /**
     * 단건 UPDATE.
     */
    private void update_AuditLogs(Object id, String encrypted) {
        String query = "UPDATE AUDIT_LOGS SET IP_ADDRESS = ? WHERE AUDIT_LOG_ID = ?";
        Connection        con   = null;
        PreparedStatement pstmt = null;
        try {
            con   = dataSource.getConnection();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, encrypted);
            pstmt.setObject(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("[MiscEncryption] AUDIT_LOGS UPDATE 실패 id={}: {}", id, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            closeQuietly(null, pstmt, con);
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
            log.warn("[MiscEncryption] DDL 실패: {} — {}", ddl, e.getMessage());
        } finally {
            closeQuietly(null, pstmt, con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 배치 조회 결과 레코드
    // ══════════════════════════════════════════════════════════════
    private record IpRow(Object id, String ip) {}

    // ══════════════════════════════════════════════════════════════
    // 리소스 정리 헬퍼
    // ══════════════════════════════════════════════════════════════
    private void closeQuietly(ResultSet rs, PreparedStatement pstmt, Connection con) {
        if (rs    != null) try { rs.close();    } catch (SQLException ignored) {}
        if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
        if (con   != null) try { con.close();   } catch (SQLException ignored) {}
    }
}