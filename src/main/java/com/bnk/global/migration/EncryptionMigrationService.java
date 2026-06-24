package com.bnk.global.migration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

	// ================================================================
	// 전체 실행 — 기존 암호화 + 신규 password_hash 복구
	// ================================================================
	public MigrationResult migrateAll() {
		log.info("[Migration] ===== 전체 마이그레이션 시작 =====");
		MigrationResult r1 = migratePhone();
		MigrationResult r2 = migrateCiValue();
		MigrationResult r3 = migrateBirthDate();
		MigrationResult r4 = recoverDoubleEncryptedPasswords(); // 신규
		MigrationResult result = new MigrationResult(
				r1.successCount() + r2.successCount() + r3.successCount() + r4.successCount(),
				r1.failCount() + r2.failCount() + r3.failCount() + r4.failCount());
		log.info("[Migration] ===== 완료 — 성공: {}건, 실패: {}건 =====", result.successCount(), result.failCount());
		return result;
	}

	// ================================================================
	// 기존: phone / ci_value / birth_date 평문 → AES 암호화
	// ================================================================
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

	// ================================================================
	// password_hash 이중 암호화 복구
	// AES(BCrypt(pw)) → 복호화 → BCrypt(pw) 로 복원
	// ================================================================
	public MigrationResult recoverDoubleEncryptedPasswords() {
		int total = migrationMapper.countDoubleEncryptedPasswordUsers();
		log.info("[Migration] password_hash 이중 암호화 복구 대상: {}건", total);
		if (total == 0) {
			log.info("[Migration] password_hash 복구 대상 없음 — 건너뜀");
			return new MigrationResult(0, 0);
		}
		int success = 0, fail = 0, offset = 0;
		while (offset < total) {
			List<MigrationUserRow> batch = migrationMapper.findDoubleEncryptedPasswordUsers(BATCH_SIZE, offset);
			if (batch.isEmpty())
				break;
			BatchResult r = recoverPasswordBatch(batch);
			success += r.success();
			fail += r.fail();
			offset += batch.size();
			log.info("[Migration] password_hash 복구 {}/{}", Math.min(offset, total), total);
		}
		log.info("[Migration] password_hash 복구 완료 — 성공: {}건, 실패: {}건", success, fail);
		return new MigrationResult(success, fail);
	}

	// ================================================================
	// Batch 처리 메서드들
	// ================================================================
	private BatchResult encryptPhoneBatch(List<MigrationUserRow> batch) {
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

	private BatchResult encryptCiValueBatch(List<MigrationUserRow> batch) {
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

	private BatchResult encryptBirthDateBatch(List<MigrationUserRow> batch) {
		int success = 0, fail = 0;
		for (MigrationUserRow row : batch) {
			try {
				String raw = row.getBirthDate();

				// 이미 AES 암호화됨 → 건너뜀
				if (aesCryptoUtil.isEncrypted(raw)) {
					success++;
					continue;
				}

				// 평문을 ISO 형식으로 정규화
				String isoDate = normalizeToIso(raw);
				if (isoDate == null) {
					log.warn("[Migration] birth_date 파싱 불가 userId={} value={}", row.getUserId(), raw);
					fail++;
					continue;
				}

				migrationMapper.updateEncryptedBirthDate(row.getUserId(), aesCryptoUtil.encrypt(isoDate));
				success++;

			} catch (Exception e) {
				log.warn("[Migration] birth_date 실패 userId={}: {}", row.getUserId(), e.getMessage());
				fail++;
			}
		}
		return new BatchResult(success, fail);
	}

	private BatchResult recoverPasswordBatch(List<MigrationUserRow> batch) {
		int success = 0, fail = 0;
		for (MigrationUserRow row : batch) {
			try {
				String stored = row.getPasswordHash();

				// ":" 없으면 이미 정상 BCrypt — 건너뜀
				if (!aesCryptoUtil.isEncrypted(stored)) {
					success++;
					continue;
				}

				// AES 복호화 → BCrypt 해시 복원
				String bcryptHash = aesCryptoUtil.decrypt(stored);

				// 복호화 결과가 BCrypt 형식인지 최소 검증 ("$2" 로 시작)
				if (bcryptHash == null || !bcryptHash.startsWith("$2")) {
					log.warn("[Migration] password_hash 복호화 결과 비정상 userId={} value={}", row.getUserId(), bcryptHash);
					fail++;
					continue;
				}

				migrationMapper.updateRestoredPassword(row.getUserId(), bcryptHash);
				log.debug("[Migration] password_hash 복구 완료 userId={}", row.getUserId());
				success++;

			} catch (Exception e) {
				log.warn("[Migration] password_hash 복구 실패 userId={}: {}", row.getUserId(), e.getMessage());
				fail++;
			}
		}
		return new BatchResult(success, fail);
	}

	/**
	 * Oracle TIMESTAMP / Oracle DATE / ISO 문자열 → "YYYY-MM-DD" 정규화
	 */
	private String normalizeToIso(String text) {
		if (text == null || text.isBlank())
			return null;

		// ISO "YYYY-MM-DD"
		try {
			return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE).toString();
		} catch (Exception ignored) {
		}

		// Oracle TIMESTAMP "16-JUL-03 12.00.00.000000000 AM"
		try {
			DateTimeFormatter oracleTsFmt = new DateTimeFormatterBuilder().parseCaseInsensitive()
					.appendPattern("dd-MMM-yy hh.mm.ss.SSSSSSSSS a").toFormatter(Locale.ENGLISH);
			return LocalDate.parse(text, oracleTsFmt).toString();
		} catch (Exception ignored) {
		}

		// Oracle DATE "16-JUL-03"
		try {
			DateTimeFormatter oracleDateFmt = new DateTimeFormatterBuilder().parseCaseInsensitive()
					.appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH);
			return LocalDate.parse(text, oracleDateFmt).toString();
		} catch (Exception ignored) {
		}

		return null;
	}
}