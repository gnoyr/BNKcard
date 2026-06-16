package com.bnk.global.migration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.bnk.global.migration.EncryptionMigrationService.MigrationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 기동 시 전체 AES 마이그레이션 자동 실행.
 *
 * [활성화 조건]
 *  application.properties 에서
 *    migration.enabled=true  → 실행
 *    migration.enabled=false → Bean 자체 생성 안 함 (완전 비활성화)
 *
 *  마이그레이션이 완전히 완료된 이후에는 false로 설정.
 *
 * [Phase 1] 명시적 암호화
 *   - USERS              : phone / ci_value / birth_date / password_hash
 *   - ADMIN_USERS        : phone
 *   - WATCHLIST          : ci_value / birth_date
 *   - USER_TRUSTED_IPS   : ip_address
 *   - EVENT_LOGS         : request_ip
 *   - LOGIN_HISTORIES    : ip_address
 *
 * [Phase 2] 전 테이블 범용 복호화 스캔
 *   - Oracle USER_TAB_COLUMNS 기반 전 컬럼 동적 스캔
 *   - 잘못 암호화된 값 탐지 → 복호화
 *
 * [멱등성] 재실행 안전.
 * [장애 격리] 각 단계 독립 try-catch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true", matchIfMissing = false)
public class EncryptionMigrationRunner implements ApplicationRunner {

    private final EncryptionMigrationService          migrationService;
    private final AdminEncryptionMigrationService     adminMigrationService;
    private final WatchlistMigrationService           watchlistMigrationService;
    private final TrustedIpMigrationService           trustedIpMigrationService;
    private final MiscEncryptionMigrationService      miscEncryptionMigrationService;
    private final UniversalDecryptionMigrationService universalDecryptionService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Migration] ══════════════════════════════════════════");
        log.info("[Migration]  서버 기동 시 AES 마이그레이션 전체 시작");
        log.info("[Migration] ══════════════════════════════════════════");

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

        runSafely("EVENT_LOGS.REQUEST_IP / LOGIN_HISTORIES.IP_ADDRESS", () -> {
            MiscEncryptionMigrationService.MigrationResult r =
                    miscEncryptionMigrationService.migrateAll();
            logResult("기타 IP 암호화", r.successCount(), r.failCount());
        });

        log.info("[Migration] ── Phase 2: 전 테이블 범용 복호화 스캔 ──");

        runSafely("전 테이블 범용 복호화", () -> {
            UniversalDecryptionMigrationService.MigrationResult r =
                    universalDecryptionService.migrateAll();
            log.info("[Migration] 범용복호화 완료 — 스캔컬럼:{}, 데이터컬럼:{}, 성공:{}, 실패:{}, skip:{}",
                    r.columnsProcessed(), r.columnsWithData(),
                    r.totalSuccess(), r.totalFail(), r.totalSkip());
        });

        log.info("[Migration] ══════════════════════════════════════════");
        log.info("[Migration]  전체 AES 마이그레이션 완료");
        log.info("[Migration]  → application.properties에서 migration.enabled=false 로 변경하세요.");
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