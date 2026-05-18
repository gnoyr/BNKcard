package com.bnk.domain.admin.mapper;

import com.bnk.domain.admin.dto.request.AdminUserSearchRequest;
import com.bnk.domain.admin.model.AdminUser;
import com.bnk.domain.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AdminUserMapper {

    Optional<AdminUser> findByUsername(@Param("username") String username);

    Optional<AdminUser> findById(@Param("adminId") Long adminId);

    /** name·email·phone LIKE + status= + birthDate BETWEEN 동적 조합 */
    List<User> findUsers(AdminUserSearchRequest request);

    long countUsers(AdminUserSearchRequest request);

    Optional<User> findUserDetailById(@Param("userId") Long userId);

    int incrementLoginFailCount(@Param("adminId") Long adminId);

    int resetLoginFailCount(@Param("adminId") Long adminId);

    int updateLockedUntil(@Param("adminId") Long adminId,
                          @Param("lockedUntil") LocalDateTime lockedUntil);

    int updateLastLoginAt(@Param("adminId") Long adminId,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /** AUDIT_LOGS INSERT */
    int insertAuditLog(@Param("actorType") String actorType,
                       @Param("actorId") Long actorId,
                       @Param("actionType") String actionType,
                       @Param("targetType") String targetType,
                       @Param("targetId") Long targetId,
                       @Param("description") String description,
                       @Param("ipAddress") String ipAddress);
}
