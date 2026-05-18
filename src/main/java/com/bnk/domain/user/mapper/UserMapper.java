package com.bnk.domain.user.mapper;

import com.bnk.domain.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface UserMapper {

    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findById(@Param("userId") Long userId);

    Optional<User> findByNameAndPhone(@Param("name") String name,
                                      @Param("phone") String phone);

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

    int resetLoginFailCount(@Param("userId") Long userId);

    int updateLockedUntil(@Param("userId") Long userId,
                          @Param("lockedUntil") LocalDateTime lockedUntil);

    int updateLastLoginAt(@Param("userId") Long userId,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /** 현재 세션 제외 타 기기 전체 revoke */
    int revokeOtherSessions(@Param("userId") Long userId,
                            @Param("currentSessionId") Long currentSessionId);

    /** 전세션 revoke — 비밀번호 재설정 후 강제 로그아웃 */
    int revokeAllSessions(@Param("userId") Long userId);
}
