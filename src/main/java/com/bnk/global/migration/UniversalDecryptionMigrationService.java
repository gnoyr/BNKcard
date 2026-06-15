package com.bnk.global.migration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniversalDecryptionMigrationService {

	private final UniversalDecryptionMigrationMapper mapper;
	private final AesCryptoUtil aesCryptoUtil;
	private final JdbcTemplate jdbcTemplate;

	private static final int BATCH_SIZE = 200;

	/**
	 * UPDATE 트리거로 차단된 로그 테이블. 일반 UPDATE 불가 → DELETE + INSERT 방식으로 처리.
	 */
	private static final Set<String> LOG_TABLES = new HashSet<>(
			Arrays.asList("AUDIT_LOGS", "ADMIN_ACTIVITY_LOG", "USER_ACTIVITY_LOG"));

	public record ColumnResult(String table, String column, int success, int fail, int skip) {
	}

	public record MigrationResult(int totalSuccess, int totalFail, int totalSkip, int columnsProcessed,
			int columnsWithData) {
	}

	// ══════════════════════════════════════════════════════════════
	// 전체 실행
	// ══════════════════════════════════════════════════════════════
	public MigrationResult migrateAll() {
		log.info("[UniversalMigration] ===== 전 테이블 AES 복호화 스캔 시작 =====");

		List<Map<String, String>> columns = mapper.findAllVarcharColumns();
		log.info("[UniversalMigration] 스캔 대상 컬럼: {}개", columns.size());

		int totalSuccess = 0, totalFail = 0, totalSkip = 0;
		int columnsProcessed = 0, columnsWithData = 0;

		for (Map<String, String> col : columns) {
			String tableName = col.get("TABLE_NAME");
			String columnName = col.get("COLUMN_NAME");
			String pkColumn = col.get("PK_COLUMN");

			columnsProcessed++;
			try {
				ColumnResult result = processColumn(tableName, columnName, pkColumn);
				totalSuccess += result.success();
				totalFail += result.fail();
				totalSkip += result.skip();
				if (result.success() + result.fail() > 0)
					columnsWithData++;
			} catch (Exception e) {
				log.error("[UniversalMigration] {}.{} 치명 예외: {}", tableName, columnName, e.getMessage(), e);
			}
		}

		log.info("[UniversalMigration] ===== 완료 ===== 처리컬럼={}, 데이터컬럼={}, 성공={}, 실패={}, skip={}", columnsProcessed,
				columnsWithData, totalSuccess, totalFail, totalSkip);

		return new MigrationResult(totalSuccess, totalFail, totalSkip, columnsProcessed, columnsWithData);
	}

	// ══════════════════════════════════════════════════════════════
	// 컬럼 단위 처리
	// ══════════════════════════════════════════════════════════════
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ColumnResult processColumn(String tableName, String columnName, String pkColumn) {

		int total;
		try {
			total = mapper.countEncryptedRows(tableName, columnName, pkColumn);
		} catch (Exception e) {
			log.error("[UniversalMigration] {}.{} COUNT 실패: {}", tableName, columnName, e.getMessage());
			return new ColumnResult(tableName, columnName, 0, 0, 0);
		}

		if (total == 0)
			return new ColumnResult(tableName, columnName, 0, 0, 0);

		log.info("[UniversalMigration] {}.{} — 후보 {}건 복호화 시작", tableName, columnName, total);

		boolean isLogTable = LOG_TABLES.contains(tableName);
		int success = 0, fail = 0, skip = 0, offset = 0;

		while (offset < total) {
			List<Map<String, Object>> batch;
			try {
				batch = mapper.findEncryptedRows(tableName, columnName, pkColumn, BATCH_SIZE, offset);
			} catch (Exception e) {
				log.error("[UniversalMigration] {}.{} SELECT 실패 offset={}: {}", tableName, columnName, offset,
						e.getMessage());
				break;
			}

			if (batch.isEmpty())
				break;

			for (Map<String, Object> row : batch) {
				Object pkValue = row.get("PK_VALUE");
				String rawValue = (String) row.get("COL_VALUE");

				if (rawValue == null || rawValue.isBlank()) {
					skip++;
					continue;
				}

				// ★ isEncrypted() 사용 안 함 — 직접 복호화 시도
				String plainText = tryDecrypt(rawValue);

				if (plainText == null) {
					// Base64 디코딩 실패 or GCM 인증 실패 = 평문이거나 다른 키로 암호화된 값
					// → 건드리지 않음
					skip++;
					continue;
				}

				if (plainText.equals(rawValue)) {
					skip++;
					continue;
				}

				// 복호화 성공 → UPDATE
				try {
					if (isLogTable) {
						// 로그 테이블: UPDATE 트리거 차단 → JdbcTemplate으로 직접 실행
						updateLogTable(tableName, columnName, pkColumn, pkValue, plainText);
					} else {
						mapper.updateDecryptedValue(tableName, columnName, pkColumn, pkValue, plainText);
					}
					success++;
				} catch (Exception e) {
					log.warn("[UniversalMigration] {}.{} pk={} — UPDATE 실패: {}", tableName, columnName, pkValue,
							e.getMessage());
					fail++;
				}
			}
			offset += batch.size();
		}

		log.info("[UniversalMigration] {}.{} 완료 — 성공:{}, 실패:{}, skip:{}", tableName, columnName, success, fail, skip);

		return new ColumnResult(tableName, columnName, success, fail, skip);
	}

	// ══════════════════════════════════════════════════════════════
	// 복호화 시도 — isEncrypted() 없이 직접 decrypt
	// ══════════════════════════════════════════════════════════════
	private String tryDecrypt(String rawValue) {
		try {
			return aesCryptoUtil.decryptDirect(rawValue);
		} catch (Exception e) {
			// 복호화 실패 = 암호문이 아닌 평문(콜론 포함 일반 텍스트) → null 반환 → skip 처리
			return null;
		}
	}

	// ══════════════════════════════════════════════════════════════
	// 로그 테이블 UPDATE — 트리거 비활성화 후 UPDATE, 재활성화
	// ══════════════════════════════════════════════════════════════
	private void updateLogTable(String tableName, String columnName, String pkColumn, Object pkValue,
			String plainText) {
		String triggerName = getExistingTriggerName(tableName);
		try {
			if (triggerName != null) {
				jdbcTemplate.execute("ALTER TRIGGER " + triggerName + " DISABLE");
			}
			jdbcTemplate.update("UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + pkColumn + " = ?",
					plainText, pkValue);
		} finally {
			if (triggerName != null) {
				try {
					jdbcTemplate.execute("ALTER TRIGGER " + triggerName + " ENABLE");
				} catch (Exception e) {
					log.error("[UniversalMigration] 트리거 재활성화 실패: {}", triggerName, e);
				}
			}
		}
	}

	/**
	 * 테이블의 UPDATE 차단 트리거가 실제 DB에 존재하는지 확인 후 반환. 존재하지 않으면 null 반환 → 트리거 비활성화 없이 일반
	 * UPDATE 진행.
	 */
	private String getExistingTriggerName(String tableName) {
// 후보 트리거명 패턴: TRG_{TABLE}_NO_UPD
		String candidate = switch (tableName) {
		case "AUDIT_LOGS" -> "TRG_AUDIT_LOGS_NO_UPD";
		case "ADMIN_ACTIVITY_LOG" -> "TRG_ADMIN_ACTIVITY_LOG_NO_UPD";
		case "USER_ACTIVITY_LOG" -> "TRG_USER_ACTIVITY_LOG_NO_UPD";
		default -> null;
		};

		if (candidate == null)
			return null;

// DB에 실제 존재하는지 확인
		try {
			Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM USER_TRIGGERS WHERE TRIGGER_NAME = ?",
					Integer.class, candidate);
			return (count != null && count > 0) ? candidate : null;
		} catch (Exception e) {
			log.warn("[UniversalMigration] 트리거 존재 확인 실패 {}: {}", candidate, e.getMessage());
			return null;
		}
	}
}