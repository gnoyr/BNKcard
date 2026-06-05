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
 * application-local.properties 에 migration.encrypt.enabled=true 추가 시 실행.
 * 완료 후 false 로 변경하거나 줄 삭제.
 * 마이그레이션 실패가 서버 기동을 막지 않도록 예외를 catch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "migration.encrypt.enabled", havingValue = "true")
public class EncryptionMigrationRunner implements ApplicationRunner {

    private final EncryptionMigrationService migrationService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Migration] 서버 기동 시 AES 마이그레이션 자동 실행");
        try {
            MigrationResult result = migrationService.migrateAll();
            if (result.failCount() > 0) {
                log.warn("[Migration] 실패 {}건 — 로그 확인 후 수동 처리 필요", result.failCount());
            }
        } catch (Exception e) {
            log.error("[Migration] 예외 발생 — 서버는 계속 기동됩니다.", e);
        }
    }
}
