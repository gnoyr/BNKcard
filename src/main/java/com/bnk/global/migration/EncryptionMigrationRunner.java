package com.bnk.global.migration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.bnk.global.migration.EncryptionMigrationService.MigrationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 기동 시 자동 실행되는 마이그레이션 Runner.
 *
 * [활성화 방법]
 * application-local.properties (또는 application.properties) 에 추가:
 *   migration.encrypt.enabled=true
 *
 * [비활성화]
 * 모든 마이그레이션 완료 후 false 로 변경하거나 해당 줄 삭제.
 * 이미 암호화된 행은 isEncrypted() 체크로 건너뛰므로 중복 실행해도 안전(멱등).
 *
 * [실행 순서]
 * 1. USERS.phone        평문 → AES 암호화
 * 2. USERS.ci_value     평문 → AES 암호화
 * 3. USERS.birth_date   평문 → AES 암호화
 * 4. USERS.password_hash 이중 암호화(AES(BCrypt)) → BCrypt 복원
 * 5. ADMIN_USERS.phone  평문 → AES 암호화  ← 신규 추가
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "migration.encrypt.enabled", havingValue = "true")
public class EncryptionMigrationRunner implements ApplicationRunner {

    private final EncryptionMigrationService      migrationService;
    private final AdminEncryptionMigrationService adminMigrationService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Migration] ===== 서버 기동 시 AES 마이그레이션 자동 실행 =====");

        // ── 1~4: USERS 테이블 (phone / ci_value / birth_date / password_hash) ──
        try {
            MigrationResult result = migrationService.migrateAll();
            if (result.failCount() > 0) {
                log.warn("[Migration] USERS 마이그레이션 실패 {}건 — 로그 확인 후 수동 처리 필요",
                        result.failCount());
            } else {
                log.info("[Migration] USERS 마이그레이션 완료 — 성공 {}건", result.successCount());
            }
        } catch (Exception e) {
            log.error("[Migration] USERS 마이그레이션 예외 발생 — 서버는 계속 기동됩니다.", e);
        }

        // ── 5: ADMIN_USERS.phone ──────────────────────────────────────────────
        try {
            AdminEncryptionMigrationService.MigrationResult adminResult =
                    adminMigrationService.migrateAdminPhone();
            if (adminResult.failCount() > 0) {
                log.warn("[Migration] ADMIN_USERS.phone 마이그레이션 실패 {}건 — 로그 확인 후 수동 처리 필요",
                        adminResult.failCount());
            } else {
                log.info("[Migration] ADMIN_USERS.phone 마이그레이션 완료 — 성공 {}건",
                        adminResult.successCount());
            }
        } catch (Exception e) {
            log.error("[Migration] ADMIN_USERS.phone 마이그레이션 예외 발생 — 서버는 계속 기동됩니다.", e);
        }

        log.info("[Migration] ===== 전체 마이그레이션 완료 =====");
    }
}