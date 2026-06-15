package com.bnk.global.migration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.bnk.global.migration.EncryptionMigrationService.MigrationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 기동 시 AES 마이그레이션 전체 자동 실행.
 *
 * [실행 순서]
 *  Phase 1 — 기존 명시적 마이그레이션 (USERS / ADMIN_USERS / WATCHLIST / USER_TRUSTED_IPS)
 *             → 이미 암호화돼야 할 컬럼들을 정상 암호화
 *
 *  Phase 2 — 전 테이블 범용 복호화 스캔
 *             → 암호화가 되면 안 되는 곳에 잘못 들어간 암호문을 전부 탐지·복호화
 *             → Phase 1 이후 실행하므로 Phase 1이 정상 암호화한 값은 건드리지 않음
 *             → 멱등: 복호화된 평문은 암호문 패턴 불충족 → 재실행 시 자동 skip
 *
 * [장애 격리]
 *  각 Phase / 테이블별 try-catch 독립 처리 → 한 단계 실패가 서버 기동을 막지 않음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionMigrationRunner implements ApplicationRunner {

    // Phase 1
    private final EncryptionMigrationService      migrationService;
    private final AdminEncryptionMigrationService adminMigrationService;
    private final WatchlistMigrationService       watchlistMigrationService;
    private final TrustedIpMigrationService       trustedIpMigrationService;

    // Phase 2
    private final UniversalDecryptionMigrationService universalDecryptionService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Migration] ══════════════════════════════════════════");
        log.info("[Migration]  서버 기동 시 AES 마이그레이션 전체 시작");
        log.info("[Migration] ══════════════════════════════════════════");

        // ── Phase 1: 암호화가 필요한 컬럼 정상 암호화 ──────────────
        log.info("[Migration] ── Phase 1: 명시적 암호화 마이그레이션 ──");

        runSafely("USERS (phone/ci_value/birth_date/password_hash)", () -> {
            MigrationResult r = migrationService.migrateAll();
            logResult("USERS", r.successCount(), r.failCount());
        });

        runSafely("ADMIN_USERS.phone", () -> {
            AdminEncryptionMigrationService.MigrationResult r =
                    adminMigrationService.migrateAdminPhone();
            logResult("ADMIN_USERS.phone", r.successCount(), r.failCount());
        });

        runSafely("WATCHLIST (ci_value/birth_date)", () -> {
            WatchlistMigrationService.MigrationResult r =
                    watchlistMigrationService.migrateAll();
            logResult("WATCHLIST", r.successCount(), r.failCount());
        });

        runSafely("USER_TRUSTED_IPS.ip_address", () -> {
            TrustedIpMigrationService.MigrationResult r =
                    trustedIpMigrationService.migrateIpAddress();
            logResult("USER_TRUSTED_IPS.ip_address", r.successCount(), r.failCount());
        });

        // ── Phase 2: 전 테이블 잘못 암호화된 값 범용 복호화 ─────────
        log.info("[Migration] ── Phase 2: 전 테이블 범용 복호화 스캔 ──");

        runSafely("전 테이블 범용 복호화", () -> {
            UniversalDecryptionMigrationService.MigrationResult r =
                    universalDecryptionService.migrateAll();
            log.info("[Migration] 범용복호화 완료 — 스캔컬럼:{}, 데이터있는컬럼:{}, 성공:{}, 실패:{}, skip:{}",
                    r.columnsProcessed(), r.columnsWithData(),
                    r.totalSuccess(), r.totalFail(), r.totalSkip());
        });

        log.info("[Migration] ══════════════════════════════════════════");
        log.info("[Migration]  전체 AES 마이그레이션 완료");
        log.info("[Migration] ══════════════════════════════════════════");
    }

    private void runSafely(String target, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("[Migration] [{}] 예외 발생 — 서버 기동 계속. error={}",
                    target, e.getMessage(), e);
        }
    }

    private void logResult(String target, int success, int fail) {
        if (fail > 0) {
            log.warn("[Migration] [{}] 완료 — 성공:{}, 실패:{} (로그 확인 필요)", target, success, fail);
        } else {
            log.info("[Migration] [{}] 완료 — 처리:{}건", target, success);
        }
    }
}