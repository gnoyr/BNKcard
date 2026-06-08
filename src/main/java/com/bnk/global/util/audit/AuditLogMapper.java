package com.bnk.global.util.audit;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 감사 로그 전용 Mapper.
 *
 * 기존 UserMapper / AdminUserMapper 각각에 흩어진 insertAuditLog()를
 * 이 Mapper 하나로 통합. AuditLogger에서만 호출.
 *
 * 기존 UserMapper.insertAuditLog() / AdminUserMapper.insertAuditLog()는
 * 즉시 제거하지 않아도 됨 — 신규 코드부터 AuditLogger 사용으로 점진 전환.
 *
 * 라우팅:
 *   insertSecurityLog   → AUDIT_LOGS         (기존 테이블, 보안 이벤트)
 *   insertUserActivity  → USER_ACTIVITY_LOG  (신규, 사용자 행위)
 *   insertAdminActivity → ADMIN_ACTIVITY_LOG (신규, 관리자 행위)
 */
@Mapper
public interface AuditLogMapper {

    /**
     * 보안 이벤트 → 기존 AUDIT_LOGS 테이블.
     * 기존 UserMapper / AdminUserMapper의 insertAuditLog와 동일한 테이블·컬럼 구조.
     */
    int insertSecurityLog(@Param("actorType")   String actorType,
                          @Param("actorId")     Long   actorId,
                          @Param("actionType")  String actionType,
                          @Param("targetType")  String targetType,
                          @Param("targetId")    Long   targetId,
                          @Param("description") String description,
                          @Param("ipAddress")   String ipAddress);

    /** 사용자 행위 → USER_ACTIVITY_LOG */
    int insertUserActivity(UserActivityLog log);

    /** 관리자 행위 → ADMIN_ACTIVITY_LOG */
    int insertAdminActivity(AdminActivityLog log);
}
