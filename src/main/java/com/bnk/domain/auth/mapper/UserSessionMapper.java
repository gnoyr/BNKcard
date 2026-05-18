package com.bnk.domain.auth.mapper;

import com.bnk.domain.auth.model.UserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserSessionMapper {

    int insertSession(UserSession session);

    /** revoked_yn='N' AND expires_at > SYSTIMESTAMP 조건 */
    Optional<UserSession> findByRefreshToken(@Param("refreshToken") String refreshToken);

    int revokeSession(@Param("sessionId") Long sessionId);

    int revokeByRefreshToken(@Param("refreshToken") String refreshToken);

    int revokeAllByUserId(@Param("userId") Long userId);

    /** 현재 세션 제외 전체 revoke (비밀번호 변경 시 타 기기 로그아웃) */
    int revokeOtherSessions(@Param("userId") Long userId,
                            @Param("currentSessionId") Long currentSessionId);
}
