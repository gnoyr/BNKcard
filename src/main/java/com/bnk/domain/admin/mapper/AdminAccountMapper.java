package com.bnk.domain.admin.mapper;

import com.bnk.domain.admin.model.AdminAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminAccountMapper {

    /** 관리자 전체 목록 조회 */
    List<AdminAccount> findAll();

    /** 관리자 단건 조회 */
    AdminAccount findById(@Param("adminId") Long adminId);

    /** 관리자 등록 */
    void insertAdmin(AdminAccount admin);

    /** 역할 변경 (ADMIN_USER_ROLES) */
    void updateRole(@Param("adminId") Long adminId,
                    @Param("roleId")  Long roleId);

    /** 상태 변경 */
    void updateStatus(@Param("adminId")    Long adminId,
                      @Param("statusCode") String statusCode);

    /** 역할 코드로 roleId 조회 */
    Long findRoleIdByCode(@Param("roleCode") String roleCode);

    /** username 중복 확인 */
    int countByUsername(@Param("username") String username);

    /** 관리자 권한 목록 조회 */
    List<String> findPermissionsByAdminId(@Param("adminId") Long adminId);
    
    /** 역할 매핑 INSERT */
    void insertUserRole(@Param("adminId")    Long adminId,
                        @Param("roleId")     Long roleId,
                        @Param("createdBy")  Long createdBy);
}