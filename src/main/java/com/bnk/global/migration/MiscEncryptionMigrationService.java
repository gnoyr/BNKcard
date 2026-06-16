package com.bnk.global.migration;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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
 *
 * [멱등성]
 *  isEncrypted() 확인 후 이미 암호화된 행은 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiscEncryptionMigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final AesCryptoUtil aesCryptoUtil;

    private static final int BATCH_SIZE = 200;

    public record MigrationResult(int successCount, int failCount) {}

    public MigrationResult migrateAll() {
        MigrationResult r1 = migrateEventLogsRequestIp();
        MigrationResult r2 = migrateLoginHistoriesIpAddress();
        return new MigrationResult(
                r1.successCount() + r2.successCount(),
                r1.failCount()    + r2.failCount());
    }

    // ── EVENT_LOGS.REQUEST_IP ──────────────────────────────────────
    @Transactional
    public MigrationResult migrateEventLogsRequestIp() {
        Integer total = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(1) FROM EVENT_LOGS
            WHERE REQUEST_IP IS NOT NULL
              AND REGEXP_COUNT(REQUEST_IP, ':') != 1
            """, Integer.class);
        int cnt = (total == null) ? 0 : total;
        log.info("[MiscEncryption] EVENT_LOGS.REQUEST_IP 평문 대상: {}건", cnt);
        if (cnt == 0) return new MigrationResult(0, 0);

        int success = 0, fail = 0, offset = 0;
        while (offset < cnt) {
            List<Map<String, Object>> batch = jdbcTemplate.queryForList(
                """
                SELECT LOG_ID, REQUEST_IP FROM EVENT_LOGS
                WHERE REQUEST_IP IS NOT NULL
                  AND REGEXP_COUNT(REQUEST_IP, ':') != 1
                ORDER BY LOG_ID
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, offset, BATCH_SIZE);

            if (batch.isEmpty()) break;

            for (Map<String, Object> row : batch) {
                Object id  = row.get("LOG_ID");
                String val = (String) row.get("REQUEST_IP");
                try {
                    if (aesCryptoUtil.isEncrypted(val)) { success++; continue; }
                    jdbcTemplate.update(
                        "UPDATE EVENT_LOGS SET REQUEST_IP = ? WHERE LOG_ID = ?",
                        aesCryptoUtil.encrypt(val), id);
                    success++;
                } catch (Exception e) {
                    log.warn("[MiscEncryption] EVENT_LOGS.REQUEST_IP id={} 실패: {}", id, e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
        }
        log.info("[MiscEncryption] EVENT_LOGS.REQUEST_IP 완료 — 성공:{}, 실패:{}", success, fail);
        return new MigrationResult(success, fail);
    }

    // ── LOGIN_HISTORIES.IP_ADDRESS ─────────────────────────────────
    @Transactional
    public MigrationResult migrateLoginHistoriesIpAddress() {
        Integer total = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(1) FROM LOGIN_HISTORIES
            WHERE IP_ADDRESS IS NOT NULL
              AND REGEXP_COUNT(IP_ADDRESS, ':') != 1
            """, Integer.class);
        int cnt = (total == null) ? 0 : total;
        log.info("[MiscEncryption] LOGIN_HISTORIES.IP_ADDRESS 평문 대상: {}건", cnt);
        if (cnt == 0) return new MigrationResult(0, 0);

        int success = 0, fail = 0, offset = 0;
        while (offset < cnt) {
            List<Map<String, Object>> batch = jdbcTemplate.queryForList(
                """
                SELECT HISTORY_ID, IP_ADDRESS FROM LOGIN_HISTORIES
                WHERE IP_ADDRESS IS NOT NULL
                  AND REGEXP_COUNT(IP_ADDRESS, ':') != 1
                ORDER BY HISTORY_ID
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, offset, BATCH_SIZE);

            if (batch.isEmpty()) break;

            for (Map<String, Object> row : batch) {
                Object id  = row.get("HISTORY_ID");
                String val = (String) row.get("IP_ADDRESS");
                try {
                    if (aesCryptoUtil.isEncrypted(val)) { success++; continue; }
                    jdbcTemplate.update(
                        "UPDATE LOGIN_HISTORIES SET IP_ADDRESS = ? WHERE HISTORY_ID = ?",
                        aesCryptoUtil.encrypt(val), id);
                    success++;
                } catch (Exception e) {
                    log.warn("[MiscEncryption] LOGIN_HISTORIES.IP_ADDRESS id={} 실패: {}", id, e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
        }
        log.info("[MiscEncryption] LOGIN_HISTORIES.IP_ADDRESS 완료 — 성공:{}, 실패:{}", success, fail);
        return new MigrationResult(success, fail);
    }
}