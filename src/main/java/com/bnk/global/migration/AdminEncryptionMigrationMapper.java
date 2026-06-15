// ── AdminEncryptionMigrationMapper.java ──────────────────────────────────────
package com.bnk.global.migration;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ADMIN_USERS 암호화 마이그레이션 전용 Mapper.
 * TypeHandler 없이 평문 그대로 읽고, 암호화된 값을 그대로 씀.
 */
@Mapper
public interface AdminEncryptionMigrationMapper {

    /** 평문 phone 건수 (콜론 1개 포함 여부로 판별) */
    int countPlainPhoneAdmins();

    /** 평문 phone 배치 조회 */
    List<AdminMigrationRow> findPlainPhoneAdmins(@Param("limit")  int limit,
                                                  @Param("offset") int offset);

    /** AES 암호화된 phone으로 UPDATE (TypeHandler 없이 raw 값 직접 저장) */
    int updateAdminPhone(@Param("adminId")       Long   adminId,
                         @Param("encryptedPhone") String encryptedPhone);
}


// ── AdminMigrationRow.java ───────────────────────────────────────────────────
// (별도 파일로 분리 가능; 여기서는 같은 패키지에 위치)
