package com.bnk.domain.admin.mapper;

import com.bnk.domain.admin.dto.request.AdminUserSearchRequest;
import com.bnk.domain.admin.model.AdminUser;
import com.bnk.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 관리자 계정 Mapper.
 */
@Mapper
public interface AdminUserMapper {

	// ── 관리자 조회 ───────────────────────────────────────────────
	Optional<AdminUser> findByUsername(@Param("username") String username);

	Optional<AdminUser> findById(@Param("adminId") Long adminId);

	// ── 회원 목록/상세 (관리자 화면 → USERS 테이블) ──────────────
	List<User> findUsers(AdminUserSearchRequest request);

	long countUsers(AdminUserSearchRequest request);

	Optional<User> findUserDetailById(@Param("userId") Long userId);

	// ── 로그인 이력 ───────────────────────────────────────────────
	int incrementLoginFailCount(@Param("adminId") Long adminId);

	int getLoginFailCount(@Param("adminId") Long adminId);

	int resetLoginFailCount(@Param("adminId") Long adminId);

	int updateLockedUntil(@Param("adminId") Long adminId, @Param("lockedUntil") LocalDateTime lockedUntil);

	int updateLastLoginAt(@Param("adminId") Long adminId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

	/**
	 * 로그인 이력 INSERT (LOGIN_HISTORIES). AuthService가 7개 개별 인수로 호출하므로 시그니처 변경 금지.
	 *
	 * @param userTypeCode    'USER' | 'ADMIN'
	 * @param userId          로그인 시도 유저/어드민 ID
	 * @param loginResultCode 'SUCCESS' | 'FAIL'
	 * @param failReason      실패 사유 (성공 시 null)
	 * @param ipAddress       클라이언트 IP
	 * @param deviceInfo      디바이스 정보
	 * @param userAgent       User-Agent 헤더
	 */
	int insertLoginHistory(@Param("userTypeCode") String userTypeCode, @Param("userId") Long userId,
			@Param("loginResultCode") String loginResultCode, @Param("failReason") String failReason,
			@Param("ipAddress") String ipAddress, @Param("deviceInfo") String deviceInfo,
			@Param("userAgent") String userAgent);

	/**
	 * 로그인 이력 최근 N건 (LOGIN_HISTORIES) — 회원 상세 화면용. user_type_code='USER' AND user_id
	 * 기준, login_at DESC
	 */
	List<LoginHistoryRow> findLoginHistoriesByUserId(@Param("userId") Long userId, @Param("limit") int limit);

	/** 약관 동의 이력 전체 (USER_TERMS_AGREEMENTS), agreed_at DESC */
	List<AgreementRow> findAgreementsByUserId(@Param("userId") Long userId);

	/** 카드 신청 이력 전체 (CARD_APPLICATIONS JOIN CARDS), applied_at DESC */
	List<ApplicationRow> findApplicationsByUserId(@Param("userId") Long userId);

	/**
	 * 대시보드 — 관리자 최근 로그인 이력 N건 (LOGIN_HISTORIES JOIN ADMIN_USERS).
	 * user_type_code='ADMIN', login_result_code='SUCCESS', login_at DESC
	 */
	List<AdminLoginRow> findRecentAdminLogins(@Param("limit") int limit);

	// ── 대시보드 카운트 ───────────────────────────────────────────
	/**
	 * 카드 상태별 건수. null 전달 시 전체 건수 반환. XML: <if test="cardStatus != null"> WHERE
	 * card_status = #{cardStatus} </if>
	 */
	long countCardsByStatus(@Param("cardStatus") String cardStatus);

	/**
	 * 회원 상태별 건수. null 전달 시 전체 건수 반환. XML: <if test="statusCode != null"> AND
	 * status_code = #{statusCode} </if>
	 */
	long countUsersByStatus(@Param("statusCode") String statusCode);

	/** 오늘 가입 회원 수 (TRUNC(created_at) = TRUNC(SYSDATE)) */
	long countTodaySignups();

	/**
	 * 약관 상태별 건수. null 전달 시 전체 건수 반환. XML: <if test="status != null"> WHERE status =
	 * #{status} </if>
	 */
	long countTermsByStatus(@Param("status") String status);

	// ── 회원 계정 관리 (→ USERS 테이블) ──────────────────────────
	int unlockUser(@Param("userId") Long userId);

	int updateUserStatus(@Param("userId") Long userId, @Param("statusCode") String statusCode);

	// ── 감사 로그 (레거시 — 신규는 AuditLogger 사용) ─────────────
	int insertAuditLog(@Param("actorType") String actorType, @Param("actorId") Long actorId,
			@Param("actionType") String actionType, @Param("targetType") String targetType,
			@Param("targetId") Long targetId, @Param("description") String description,
			@Param("ipAddress") String ipAddress);

	/** ★ 신규: 관리자 계정 INSERT. phone 컬럼에 aesTypeHandler 적용 (XML 참고). */
	int insertAdmin(AdminUser adminUser);

	/** 롤 코드 목록 조회 (adminMap collection 서브쿼리용) */
	List<String> findRoleCodesByAdminId(@Param("adminId") Long adminId);

	// ── Inner Row 클래스 — MyBatis resultMap 매핑용 ──────────────

	/** LOGIN_HISTORIES 조회 결과 (유저 기준) */
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	class LoginHistoryRow {
		private LocalDateTime loginAt;
		private String loginResultCode;
		private String ipAddress;
	}

	/** USER_TERMS_AGREEMENTS 조회 결과 */
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	class AgreementRow {
		private Long termsId;
		private String agreedYn;
		private LocalDateTime agreedAt;
	}

	/** CARD_APPLICATIONS JOIN CARDS 조회 결과 */
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	class ApplicationRow {
		private Long cardId;
		private String cardName;
		private String applicationStatus;
		private LocalDateTime appliedAt;
	}

	/** LOGIN_HISTORIES JOIN ADMIN_USERS 조회 결과 (대시보드용) */
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	class AdminLoginRow {
		private String adminName;
		private LocalDateTime loginAt;
		private String ipAddress;
	}
}