package com.bnk.global.migration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 기동 시 기존 회원 CI 일괄 재생성.
 *
 * [활성화] application.properties 에서
 *   migration.ci.enabled=true  → 1회 실행 후 false 로 변경
 *
 * AES 마이그레이션(migration.enabled)과 독립적으로 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "migration.ci.enabled", havingValue = "true", matchIfMissing = false)
public class CiRecomputeMigrationRunner implements ApplicationRunner {

    private final CiRecomputeMigrationService service;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[CiMigration] ── 기존 회원 CI 재생성 시작 (이름 + 생년월일 + 전화번호) ──");
        CiRecomputeMigrationService.MigrationResult r = service.migrateAll();
        log.info("[CiMigration] 완료 — 성공:{}, 실패:{}, skip:{}  → migration.ci.enabled=false 로 변경하세요.",
                r.successCount(), r.failCount(), r.skipCount());
    }
}
