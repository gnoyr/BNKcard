package com.bnk.global.migration;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ADMIN_USERS.phone 평문 → AES-256-GCM 암호화 마이그레이션 서비스.
 *
 * [배경]
 *  - ADMIN_USERS 시드 데이터의 phone 컬럼이 평문(예: "010-9001-0001")으로 삽입됨.
 *  - AdminUserMapper.xml resultMap(adminMap)에서 phone을 aesTypeHandler로 복호화 시도.
 *  - isEncrypted() 강화 후 오탐은 제거됐으나, 평문 저장 자체는 보안 위반.
 *  - 이 서비스가 평문 phone을 AES 암호화하여 덮어쓴다.
 *
 * [실행 방법]
 *  POST /api/admin/migration/encrypt-admin-phone (SUPER_ADMIN 권한)
 *  또는 AdminEncryptionMigrationRunner에서 자동 실행.
 *
 * [멱등성]
 *  AesCryptoUtil.isEncrypted() 로 이미 암호화된 행은 건너뜀.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEncryptionMigrationService {

    private final AdminEncryptionMigrationMapper adminMigrationMapper;
    private final AesCryptoUtil aesCryptoUtil;

    private static final int BATCH_SIZE = 50;

    public record MigrationResult(int successCount, int failCount) {}

    // ================================================================
    // ADMIN_USERS.phone 마이그레이션
    // ================================================================
    public MigrationResult migrateAdminPhone() {
        int total   = adminMigrationMapper.countPlainPhoneAdmins();
        log.info("[AdminMigration] phone 평문 대상: {}건", total);

        int success = 0, fail = 0, offset = 0;

        while (offset < total) {
            List<AdminMigrationRow> batch =
                adminMigrationMapper.findPlainPhoneAdmins(BATCH_SIZE, offset);

            if (batch.isEmpty()) break;

            for (AdminMigrationRow row : batch) {
                try {
                    // 이미 암호화된 경우 스킵 (멱등성 보장)
                    if (aesCryptoUtil.isEncrypted(row.getPhone())) {
                        log.debug("[AdminMigration] adminId={} — phone 이미 암호화됨, 스킵", row.getAdminId());
                        success++;
                        continue;
                    }
                    String encrypted = aesCryptoUtil.encrypt(row.getPhone());
                    updateAdminPhoneSingleTx(row.getAdminId(), encrypted);
                    success++;
                } catch (Exception e) {
                    log.error("[AdminMigration] adminId={} phone 암호화 실패", row.getAdminId(), e);
                    fail++;
                }
            }
            offset += batch.size();
        }

        log.info("[AdminMigration] phone 완료 — 성공: {}건, 실패: {}건", success, fail);
        return new MigrationResult(success, fail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAdminPhoneSingleTx(Long adminId, String encryptedPhone) {
        adminMigrationMapper.updateAdminPhone(adminId, encryptedPhone);
    }
}
