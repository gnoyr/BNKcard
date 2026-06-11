package com.bnk.global.migration;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.global.migration.EncryptionMigrationService.MigrationResult;
import com.bnk.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 마이그레이션 수동 트리거 API.
 * Runner 대신 수동으로 실행하거나 부분 실패 건 재처리 시 사용.
 *
 * POST /api/admin/migration/encrypt-all    전체 실행
 * POST /api/admin/migration/encrypt-phone  phone 만
 * POST /api/admin/migration/encrypt-ci     ci_value 만
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMigrationController {

    private final EncryptionMigrationService migrationService;

    @PostMapping("/encrypt-all")
    public ResponseEntity<ApiResponse<MigrationSummary>> encryptAll() {
        log.info("[Migration API] 전체 암호화 수동 트리거");
        MigrationResult result = migrationService.migrateAll();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result, "전체")));
    }

    @PostMapping("/encrypt-phone")
    public ResponseEntity<ApiResponse<MigrationSummary>> encryptPhone() {
        log.info("[Migration API] phone 암호화 수동 트리거");
        MigrationResult result = migrationService.migratePhone();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result, "phone")));
    }

    @PostMapping("/encrypt-ci")
    public ResponseEntity<ApiResponse<MigrationSummary>> encryptCiValue() {
        log.info("[Migration API] ci_value 암호화 수동 트리거");
        MigrationResult result = migrationService.migrateCiValue();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result, "ci_value")));
    }
    
    @PostMapping("/recover-password")
    public ResponseEntity<ApiResponse<MigrationSummary>> recoverPassword() {
        log.info("[Migration API] password_hash 이중 암호화 복구 수동 트리거");
        MigrationResult result = migrationService.recoverDoubleEncryptedPasswords();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result, "password_hash 복구")));
    }

    private MigrationSummary toSummary(MigrationResult result, String target) {
        return new MigrationSummary(
            target,
            result.successCount(),
            result.failCount(),
            result.failCount() == 0 ? "완료" : "부분 완료 — 로그 확인 필요"
        );
    }

    public record MigrationSummary(String target, int successCount, int failCount, String status) {}
}