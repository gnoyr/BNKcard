package com.bnk.global.migration;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EncryptionMigrationMapper {

	// ── 기존 (phone / ci_value / birth_date) ─────────────────────────
	List<MigrationUserRow> findPlainPhoneUsers(@Param("batchSize") int batchSize, @Param("offset") int offset);

	List<MigrationUserRow> findPlainCiValueUsers(@Param("batchSize") int batchSize, @Param("offset") int offset);

	List<MigrationUserRow> findPlainBirthDateUsers(@Param("batchSize") int batchSize, @Param("offset") int offset);

	int updateEncryptedPhone(@Param("userId") long userId, @Param("encryptedPhone") String encryptedPhone);

	int updateEncryptedCiValue(@Param("userId") long userId, @Param("encryptedCiValue") String encryptedCiValue);

	int updateEncryptedBirthDate(@Param("userId") long userId, @Param("encryptedBirthDate") String encryptedBirthDate);

	int countPlainPhoneUsers();

	int countPlainCiValueUsers();

	int countPlainBirthDateUsers();

	List<MigrationUserRow> findDoubleEncryptedPasswordUsers(@Param("batchSize") int batchSize,
			@Param("offset") int offset);

	int countDoubleEncryptedPasswordUsers();

	int updateRestoredPassword(@Param("userId") long userId, @Param("passwordHash") String passwordHash);
}