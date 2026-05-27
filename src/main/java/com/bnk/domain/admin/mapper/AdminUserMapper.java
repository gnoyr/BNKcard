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

@Mapper
public interface AdminUserMapper {

    Optional<AdminUser> findByUsername(@Param("username") String username);

    Optional<AdminUser> findById(@Param("adminId") Long adminId);

    List<User> findUsers(AdminUserSearchRequest request);

    long countUsers(AdminUserSearchRequest request);

    Optional<User> findUserDetailById(@Param("userId") Long userId);

    int incrementLoginFailCount(@Param("adminId") Long adminId);

    int getLoginFailCount(@Param("adminId") Long adminId);

    int resetLoginFailCount(@Param("adminId") Long adminId);

    int updateLockedUntil(@Param("adminId") Long adminId,
                          @Param("lockedUntil") LocalDateTime lockedUntil);

    int updateLastLoginAt(@Param("adminId") Long adminId,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt);

    int unlockUser(@Param("userId") Long userId);
    
    int updateUserStatus(@Param("userId") Long userId, @Param("statusCode") String statusCode);

    int insertAuditLog(@Param("actorType")  String actorType,
                       @Param("actorId")    Long   actorId,
                       @Param("actionType") String actionType,
                       @Param("targetType") String targetType,
                       @Param("targetId")   Long   targetId,
                       @Param("description")String description,
                       @Param("ipAddress")  String ipAddress);

    /**
     * 로그인 이력 INSERT (LOGIN_HISTORIES)
     * 유저/관리자 로그인 성공·실패 시 공통 호출.
     * userTypeCode = 'USER' | 'ADMIN'
     * loginResultCode = 'SUCCESS' | 'FAIL'
     */
    int insertLoginHistory(@Param("userTypeCode")   String userTypeCode,
                           @Param("userId")          Long   userId,
                           @Param("loginResultCode") String loginResultCode,
                           @Param("failReason")      String failReason,
                           @Param("ipAddress")       String ipAddress,
                           @Param("deviceInfo")      String deviceInfo,
                           @Param("userAgent")       String userAgent);

    /**
     * 로그인 이력 최근 N건 (LOGIN_HISTORIES)
     * user_type_code='USER' AND user_id 기준, login_at DESC
     *
     * @param userId 대상 유저 ID
     * @param limit  조회 건수 (상세 조회: 5건 고정)
     */
    List<LoginHistoryRow> findLoginHistoriesByUserId(
            @Param("userId") Long userId,
            @Param("limit")  int  limit);

    /**
     * 약관 동의 이력 전체 (USER_TERMS_AGREEMENTS)
     * agreed_at DESC 정렬
     *
     * @param userId 대상 유저 ID
     */
    List<AgreementRow> findAgreementsByUserId(@Param("userId") Long userId);

    /**
     * 카드 신청 이력 전체 (CARD_APPLICATIONS JOIN CARDS)
     * applied_at DESC 정렬
     *
     * @param userId 대상 유저 ID
     */
    List<ApplicationRow> findApplicationsByUserId(@Param("userId") Long userId);

    /**
     * 대시보드 — 관리자 최근 로그인 이력 N건 (LOGIN_HISTORIES JOIN ADMIN_USERS)
     * user_type_code='ADMIN', login_result_code='SUCCESS', login_at DESC
     *
     * @param limit 조회 건수 (대시보드: 5건 고정)
     */
    List<AdminLoginRow> findRecentAdminLogins(@Param("limit") int limit);

    // ----------------------------------------------------------------
    // Inner Row 클래스 — MyBatis resultMap 매핑용
    // ----------------------------------------------------------------

    /** LOGIN_HISTORIES 조회 결과 (유저 기준) */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    class LoginHistoryRow {
        private LocalDateTime loginAt;
        private String        loginResultCode;
        private String        ipAddress;
    }

    /** USER_TERMS_AGREEMENTS 조회 결과 */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    class AgreementRow {
        private Long          termsId;
        private String        agreedYn;
        private LocalDateTime agreedAt;
    }

    /** CARD_APPLICATIONS JOIN CARDS 조회 결과 */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    class ApplicationRow {
        private Long          cardId;
        private String        cardName;
        private String        applicationStatus;
        private LocalDateTime appliedAt;
    }

    /** LOGIN_HISTORIES JOIN ADMIN_USERS 조회 결과 (대시보드용) */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    class AdminLoginRow {
        private String        adminName;
        private LocalDateTime loginAt;
        private String        ipAddress;
    }
}
