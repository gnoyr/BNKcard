package com.bnk.global.migration;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기존 평문 데이터 AES 암호화 마이그레이션 서비스. - 100건 단위 배치 처리 (OOM 방지) - 배치별 독립 트랜잭션 (중간 실패 시
 * 해당 배치만 롤백) - isEncrypted() 체크로 이중 암호화 방지 - 실패 건은 WARN 로그 후 skip (전체 중단 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptionMigrationService {

	private final EncryptionMigrationMapper migrationMapper;
	private final AesCryptoUtil aesCryptoUtil;

	private static final int BATCH_SIZE = 100;

	public record MigrationResult(int successCount, int failCount) {
		public static MigrationResult merge(MigrationResult a, MigrationResult b) {
			return new MigrationResult(a.successCount + b.successCount, a.failCount + b.failCount);
		}
	}

	private record BatchResult(int success, int fail) {
	}

	public MigrationResult migrateAll() {
		log.info("[Migration] ===== AES 암호화 마이그레이션 시작 =====");
		MigrationResult r1 = migratePhone();
		MigrationResult r2 = migrateCiValue();
		MigrationResult r3 = migrateBirthDate();
		MigrationResult result = new MigrationResult(r1.successCount() + r2.successCount() + r3.successCount(),
				r1.failCount() + r2.failCount() + r3.failCount());
		log.info("[Migration] ===== 완료 — 성공: {}건, 실패: {}건 =====", result.successCount(), result.failCount());
		return result;
	}

	public MigrationResult migratePhone() {
		int total = migrationMapper.countPlainPhoneUsers();
		log.info("[Migration] phone 대상: {}건", total);
		int success = 0, fail = 0, offset = 0;
		while (offset < total) {
			List<MigrationUserRow> batch = migrationMapper.findPlainPhoneUsers(BATCH_SIZE, offset);
			if (batch.isEmpty())
				break;
			BatchResult r = encryptPhoneBatch(batch);
			success += r.success();
			fail += r.fail();
			offset += batch.size();
			log.info("[Migration] phone {}/{}", Math.min(offset, total), total);
		}
		return new MigrationResult(success, fail);
	}

	public MigrationResult migrateCiValue() {
		int total = migrationMapper.countPlainCiValueUsers();
		log.info("[Migration] ci_value 대상: {}건", total);
		int success = 0, fail = 0, offset = 0;
		while (offset < total) {
			List<MigrationUserRow> batch = migrationMapper.findPlainCiValueUsers(BATCH_SIZE, offset);
			if (batch.isEmpty())
				break;
			BatchResult r = encryptCiValueBatch(batch);
			success += r.success();
			fail += r.fail();
			offset += batch.size();
			log.info("[Migration] ci_value {}/{}", Math.min(offset, total), total);
		}
		return new MigrationResult(success, fail);
	}

	public MigrationResult migrateBirthDate() {
		int total = migrationMapper.countPlainBirthDateUsers();
		log.info("[Migration] birth_date 대상: {}건", total);
		int success = 0, fail = 0, offset = 0;
		while (offset < total) {
			List<MigrationUserRow> batch = migrationMapper.findPlainBirthDateUsers(BATCH_SIZE, offset);
			if (batch.isEmpty())
				break;
			BatchResult r = encryptBirthDateBatch(batch);
			success += r.success();
			fail += r.fail();
			offset += batch.size();
			log.info("[Migration] birth_date {}/{}", Math.min(offset, total), total);
		}
		return new MigrationResult(success, fail);
	}

	@Transactional
	public BatchResult encryptPhoneBatch(List<MigrationUserRow> batch) {
		int success = 0, fail = 0;
		for (MigrationUserRow row : batch) {
			try {
				if (aesCryptoUtil.isEncrypted(row.getPhone())) {
					success++;
					continue;
				}
				migrationMapper.updateEncryptedPhone(row.getUserId(), aesCryptoUtil.encrypt(row.getPhone()));
				success++;
			} catch (Exception e) {
				log.warn("[Migration] phone 실패 userId={}: {}", row.getUserId(), e.getMessage());
				fail++;
			}
		}
		return new BatchResult(success, fail);
	}

	@Transactional
	public BatchResult encryptCiValueBatch(List<MigrationUserRow> batch) {
		int success = 0, fail = 0;
		for (MigrationUserRow row : batch) {
			try {
				if (aesCryptoUtil.isEncrypted(row.getCiValue())) {
					success++;
					continue;
				}
				migrationMapper.updateEncryptedCiValue(row.getUserId(), aesCryptoUtil.encrypt(row.getCiValue()));
				success++;
			} catch (Exception e) {
				log.warn("[Migration] ci_value 실패 userId={}: {}", row.getUserId(), e.getMessage());
				fail++;
			}
		}
		return new BatchResult(success, fail);
	}

	@Transactional
	public BatchResult encryptBirthDateBatch(List<MigrationUserRow> batch) {
		int success = 0, fail = 0;
		for (MigrationUserRow row : batch) {
			try {
				if (aesCryptoUtil.isEncrypted(row.getBirthDate())) {
					success++;
					continue;
				}
				migrationMapper.updateEncryptedBirthDate(row.getUserId(), aesCryptoUtil.encrypt(row.getBirthDate()));
				success++;
			} catch (Exception e) {
				log.warn("[Migration] birth_date 실패 userId={}: {}", row.getUserId(), e.getMessage());
				fail++;
			}
		}
		return new BatchResult(success, fail);
	}
}
