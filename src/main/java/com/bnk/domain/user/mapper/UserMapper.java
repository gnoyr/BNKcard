package com.bnk.domain.user.mapper;

import com.bnk.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    // ----------------------------------------------------------------
    // 기존 메서드 (변경 없음)
    // ----------------------------------------------------------------

    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findById(@Param("userId") Long userId);

    Optional<User> findByNameAndPhone(@Param("name") String name,
                                      @Param("phone") String phone);

    int existsByEmail(@Param("email") String email);

    int existsByPhone(@Param("phone") String phone);

    int insertUser(User user);

    int updateUser(User user);

    int updateEmailVerified(@Param("userId") Long userId,
                            @Param("isEmailVerified") String isEmailVerified);

    int updatePhoneVerified(@Param("userId") Long userId,
                            @Param("isPhoneVerified") String isPhoneVerified);

    int updatePassword(@Param("userId") Long userId,
                       @Param("passwordHash") String passwordHash,
                       @Param("lastPasswordChangedAt") LocalDateTime lastPasswordChangedAt);

    int incrementLoginFailCount(@Param("userId") Long userId);

    int getLoginFailCount(@Param("userId") Long userId);

    int resetLoginFailCount(@Param("userId") Long userId);

    int updateLockedUntil(@Param("userId") Long userId,
                          @Param("lockedUntil") LocalDateTime lockedUntil);

    int updateLastLoginAt(@Param("userId") Long userId,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt);

    int revokeOtherSessions(@Param("userId") Long userId,
                            @Param("currentSessionId") Long currentSessionId);

    int revokeAllSessions(@Param("userId") Long userId);

    // ----------------------------------------------------------------
    // 추가 — AUDIT_LOGS INSERT (마이페이지 전역 사용)
    // ----------------------------------------------------------------

    /**
     * AUDIT_LOGS INSERT.
     * actor_type_code('USER'/'ADMIN'), actor_id, action_type_code,
     * target_type_code, target_id, description, ip_address
     */
    int insertAuditLog(@Param("actorType")  String actorType,
                       @Param("actorId")    Long   actorId,
                       @Param("actionType") String actionType,
                       @Param("targetType") String targetType,
                       @Param("targetId")   Long   targetId,
                       @Param("description")String description,
                       @Param("ipAddress")  String ipAddress);

    // ----------------------------------------------------------------
    // 추가 — 보유 카드 및 신청 현황 (RQ-F17)
    // ----------------------------------------------------------------

    /**
     * 발급 완료 카드 목록
     * USER_CARDS JOIN CARDS JOIN CARD_IMAGES(image_type='FRONT')
     * WHERE USER_CARDS.deleted_yn='N'
     */
    List<OwnedCardRow> selectOwnedCards(@Param("userId") Long userId);

    /**
     * 카드 신청 현황 목록
     * CARD_APPLICATIONS JOIN CARDS JOIN CARD_IMAGES(image_type='THUMBNAIL')
     */
    List<CardApplicationRow> selectCardApplications(@Param("userId") Long userId);

    // ----------------------------------------------------------------
    // 추가 — 소비 패턴 (RQ-F16, RQ-F18)
    // ----------------------------------------------------------------

    /**
     * 소비 패턴 조회 (RQ-F16)
     * CARD_CATEGORIES LEFT JOIN USER_SPENDING_PATTERNS
     * 패턴 없는 카테고리도 0원으로 포함
     */
    List<SpendingPatternRow> selectSpendingPatterns(@Param("userId") Long userId);

    /**
     * 소비 패턴 UPSERT (RQ-F18) — Oracle MERGE INTO
     * 존재: monthly_amount, updated_at UPDATE
     * 없음: INSERT (source='MANUAL' 고정)
     */
    int upsertSpendingPattern(@Param("userId")        Long       userId,
                               @Param("categoryId")    Long       categoryId,
                               @Param("monthlyAmount") BigDecimal monthlyAmount,
                               @Param("source")        String     source);

    // ----------------------------------------------------------------
    // Inner Row 클래스 — MyBatis resultMap 매핑용
    // ----------------------------------------------------------------

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    class OwnedCardRow {
        private Long   userCardId;
        private Long   cardId;
        private String cardName;
        private String cardImageUrl;
        private String issuedAt;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    class CardApplicationRow {
        private Long   applicationId;
        private Long   cardId;
        private String cardName;
        private String cardImageUrl;
        private String applicationStatus;
        private String appliedAt;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    class SpendingPatternRow {
        private Long       categoryId;
        private String     categoryName;
        private BigDecimal monthlyAmount;
        private String     colorCode;
    }
}
