package com.bnk.domain.user.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.user.dto.query.CardApplicationRow;
import com.bnk.domain.user.dto.query.OwnedCardRow;
import com.bnk.domain.user.dto.query.SpendingPatternRow;
import com.bnk.domain.user.model.User;

@Mapper
public interface UserMapper {

	Optional<User> findByEmail(@Param("email") String email);

	Optional<User> findById(@Param("userId") Long userId);

	List<User> findByName(@Param("name") String name);

	Optional<User> findByEmailAndName(@Param("email") String email, @Param("name") String name);

	int existsByEmail(@Param("email") String email);

	List<User> findAllPhones();

	int insertUser(User user);

	int updateUser(User user);

	int updateEmailVerified(@Param("userId") Long userId, @Param("isEmailVerified") String isEmailVerified);

	int updatePhoneVerified(@Param("userId") Long userId, @Param("isPhoneVerified") String isPhoneVerified);

	int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash,
			@Param("lastPasswordChangedAt") LocalDateTime lastPasswordChangedAt);

	int incrementLoginFailCount(@Param("userId") Long userId);

	int getLoginFailCount(@Param("userId") Long userId);

	int resetLoginFailCount(@Param("userId") Long userId);

	int updateLockedUntil(@Param("userId") Long userId, @Param("lockedUntil") LocalDateTime lockedUntil);

	int updateLastLoginAt(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

	int revokeOtherSessions(@Param("userId") Long userId, @Param("currentSessionId") Long currentSessionId);

	int revokeAllSessions(@Param("userId") Long userId);

	int insertAuditLog(@Param("actorType") String actorType, @Param("actorId") Long actorId,
			@Param("actionType") String actionType, @Param("targetType") String targetType,
			@Param("targetId") Long targetId, @Param("description") String description,
			@Param("ipAddress") String ipAddress);

	// 보유 카드 및 신청 현황
	List<OwnedCardRow> selectOwnedCards(@Param("userId") Long userId);

	List<CardApplicationRow> selectCardApplications(@Param("userId") Long userId);

	List<SpendingPatternRow> selectSpendingPatterns(@Param("userId") Long userId);

	int upsertSpendingPattern(@Param("userId") Long userId, @Param("categoryId") Long categoryId,
			@Param("monthlyAmount") BigDecimal monthlyAmount, @Param("source") String source);

	// 비밀번호 이력
	int insertPasswordHistory(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

	List<String> findRecentPasswordHashes(@Param("userId") Long userId, @Param("limit") int limit);

	int deleteOldPasswordHistories(@Param("userId") Long userId);

	// CDD 상태 변경
	int updateCddStatus(@Param("userId") Long userId, @Param("cddStatusCode") String cddStatusCode);
}
