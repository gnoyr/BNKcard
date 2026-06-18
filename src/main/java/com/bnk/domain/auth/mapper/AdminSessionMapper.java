package com.bnk.domain.auth.mapper;

import com.bnk.domain.auth.model.AdminSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface AdminSessionMapper {

    int insertSession(AdminSession session);

    Optional<AdminSession> findByRefreshToken(@Param("refreshToken") String refreshToken);

    /** revoke 여부 무관 조회 — 탈취 감지용 */
    Optional<AdminSession> findAnyByRefreshToken(@Param("refreshToken") String refreshToken);

    int revokeAllByAdminId(@Param("adminId") Long adminId);
}