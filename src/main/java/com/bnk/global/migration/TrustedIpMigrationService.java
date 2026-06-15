package com.bnk.global.migration;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * USER_TRUSTED_IPS 테이블 AES 암호화 마이그레이션 서비스.
 *
 * 대상 컬럼:
 *  - ip_address: 평문 IP → AES-256-GCM 암호화
 *
 * 멱등성: isEncrypted() 확인 후 이미 암호화된 행은 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrustedIpMigrationService {

    private final TrustedIpMigrationMapper mapper;
    private final AesCryptoUtil            aesCryptoUtil;

    private static final int BATCH_SIZE = 100;

    public record MigrationResult(int successCount, int failCount) {}

    @Transactional
    public MigrationResult migrateIpAddress() {
        int total = mapper.countPlainIpAddresses();
        log.info("[TrustedIpMigration] ip_address 대상: {}건", total);
        if (total == 0) return new MigrationResult(0, 0);

        int success = 0, fail = 0, offset = 0;
        while (offset < total) {
            List<TrustedIpMigrationRow> batch =
                    mapper.findPlainIpAddresses(BATCH_SIZE, offset);
            if (batch.isEmpty()) break;

            for (TrustedIpMigrationRow row : batch) {
                try {
                    if (aesCryptoUtil.isEncrypted(row.getIpAddress())) { success++; continue; }
                    mapper.updateIpAddress(
                            row.getTrustId(),
                            aesCryptoUtil.encrypt(row.getIpAddress()));
                    success++;
                } catch (Exception e) {
                    log.warn("[TrustedIpMigration] ip_address 실패 trustId={}: {}",
                            row.getTrustId(), e.getMessage());
                    fail++;
                }
            }
            offset += batch.size();
            log.info("[TrustedIpMigration] ip_address {}/{}", Math.min(offset, total), total);
        }
        log.info("[TrustedIpMigration] ip_address 완료 — 성공: {}건, 실패: {}건", success, fail);
        return new MigrationResult(success, fail);
    }
}