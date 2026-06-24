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
 *
 * [수정 이력]
 *   - POST /api/admin/migration/encrypt-admin-phone 추가
 *     ADMIN_USERS.phone 평문 → AES 암호화
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMigrationController {

    private final EncryptionMigrationService      migrationService;
    private final AdminEncryptionMigrationService adminMigrationService;

    @PostMapping("/encrypt-all")
    public ResponseEntity<ApiResponse<MigrationSummary>> encryptAll() {
        log.info("[Migration API] 전체 암호화 수동 트리거");
        MigrationResult result = migrationService.migrateAll();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result.successCount(), result.failCount(), "전체")));
    }

    @PostMapping("/encrypt-phone")
    public ResponseEntity<ApiResponse<MigrationSummary>> encryptPhone() {
        log.info("[Migration API] phone 암호화 수동 트리거");
        MigrationResult result = migrationService.migratePhone();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result.successCount(), result.failCount(), "phone")));
    }

    @PostMapping("/encrypt-ci")
    public ResponseEntity<ApiResponse<MigrationSummary>> encryptCiValue() {
        log.info("[Migration API] ci_value 암호화 수동 트리거");
        MigrationResult result = migrationService.migrateCiValue();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result.successCount(), result.failCount(), "ci_value")));
    }

    @PostMapping("/recover-password")
    public ResponseEntity<ApiResponse<MigrationSummary>> recoverPassword() {
        log.info("[Migration API] password_hash 이중 암호화 복구 수동 트리거");
        MigrationResult result = migrationService.recoverDoubleEncryptedPasswords();
        return ResponseEntity.ok(ApiResponse.ok(toSummary(result.successCount(), result.failCount(), "password_hash 복구")));
    }

    /**
     * ★ 신규: ADMIN_USERS.phone 평문 → AES 암호화.
     * 시드 데이터 또는 기존 insertAdmin() 없이 삽입된 평문 phone 처리.
     */
    @PostMapping("/encrypt-admin-phone")
    public ResponseEntity<ApiResponse<MigrationSummary>> encryptAdminPhone() {
        log.info("[Migration API] ADMIN_USERS.phone 암호화 수동 트리거");
        AdminEncryptionMigrationService.MigrationResult result =
            adminMigrationService.migrateAdminPhone();
        return ResponseEntity.ok(ApiResponse.ok(
            toSummary(result.successCount(), result.failCount(), "ADMIN_USERS.phone")));
    }

    private MigrationSummary toSummary(int successCount, int failCount, String target) {
        return new MigrationSummary(
            target,
            successCount,
            failCount,
            failCount == 0 ? "완료" : "부분 완료 — 로그 확인 필요"
        );
    }

    public record MigrationSummary(String target, int successCount, int failCount, String status) {}
}
