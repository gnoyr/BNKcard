package com.bnk.global.migration;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WATCHLIST 테이블 AES 암호화 마이그레이션 서비스.
 *
 * 대상 컬럼:
 *  - ci_value  : 평문 CI값 → AES-256-GCM 암호화
 *  - birth_date: 평문 "YYYY-MM-DD" → AES-256-GCM 암호화
 *
 * 멱등성: isEncrypted() 확인 후 이미 암호화된 행은 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistMigrationService {

    private final WatchlistMigrationMapper mapper;
    private final AesCryptoUtil            aesCryptoUtil;

    private static final int BATCH_SIZE = 100;

    public record MigrationResult(int successCount, int failCount) {}

    // ── 전체 실행 ──────────────────────────────────────────────────
    public MigrationResult migrateAll() {
        MigrationResult r1 = migrateCiValue();
        MigrationResult r2 = migrateBirthDate();
        return new MigrationResult(
                r1.successCount() + r2.successCount(),
                r1.failCount()    + r2.failCount());
    }

    // ── ci_value ───────────────────────────────────────────────────
    @Transactional
    public MigrationResult migrateCiValue() {
        int total = mapper.countPlainCiValueWatchlists();
        log.info("[WatchlistMigration] ci_value 대상: {}건", total);
        if (total == 0) return new MigrationResult(0, 0);

        int success = 0, fail = 0, offset = 0;
        while (offset < total) {
            List<WatchlistMigrationRow> batch =
                    mapper.findPlainCiValueWatchlists(BATCH_SIZE, offset);
            if (batch.isEmpty()) break;

            for (WatchlistMigrationRow row : batch) {
                try {
                    if (aesCryptoUtil.isEncrypted(row.getCiValue())) { success++; continue; }
                    mapper.updateWatchlistCiValue(
                            row.getWatchlistId(),
                            aesCryptoUtil.encrypt(row.getCiValue()));
                    success++;
                } catch (Exception e) {
                    log.warn("[WatchlistMigration] ci_value 실패 watchlistId={}: {}",
                            row.getWatchlistId(), e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
            log.info("[WatchlistMigration] ci_value {}/{}", Math.min(offset, total), total);
        }
        log.info("[WatchlistMigration] ci_value 완료 — 성공: {}건, 실패: {}건", success, fail);
        return new MigrationResult(success, fail);
    }

    // ── birth_date ─────────────────────────────────────────────────
    @Transactional
    public MigrationResult migrateBirthDate() {
        int total = mapper.countPlainBirthDateWatchlists();
        log.info("[WatchlistMigration] birth_date 대상: {}건", total);
        if (total == 0) return new MigrationResult(0, 0);

        int success = 0, fail = 0, offset = 0;
        while (offset < total) {
            List<WatchlistMigrationRow> batch =
                    mapper.findPlainBirthDateWatchlists(BATCH_SIZE, offset);
            if (batch.isEmpty()) break;

            for (WatchlistMigrationRow row : batch) {
                try {
                    if (aesCryptoUtil.isEncrypted(row.getBirthDate())) { success++; continue; }
                    mapper.updateWatchlistBirthDate(
                            row.getWatchlistId(),
                            aesCryptoUtil.encrypt(row.getBirthDate()));
                    success++;
                } catch (Exception e) {
                    log.warn("[WatchlistMigration] birth_date 실패 watchlistId={}: {}",
                            row.getWatchlistId(), e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
            log.info("[WatchlistMigration] birth_date {}/{}", Math.min(offset, total), total);
        }
        log.info("[WatchlistMigration] birth_date 완료 — 성공: {}건, 실패: {}건", success, fail);
        return new MigrationResult(success, fail);
    }
}