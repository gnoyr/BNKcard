package com.bnk.domain.admin.service;

import com.bnk.domain.admin.dto.request.AdminCreateRequest;
import com.bnk.domain.admin.dto.request.AdminRoleUpdateRequest;
import com.bnk.domain.admin.dto.response.AdminAccountResponse;
import com.bnk.domain.admin.mapper.AdminAccountMapper;
import com.bnk.domain.admin.model.AdminAccount;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AdminAccountMapper adminAccountMapper;
    private final PasswordEncoder    passwordEncoder;

    /**
     * 관리자 전체 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AdminAccountResponse> getAdminList() {
        return adminAccountMapper.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 관리자 단건 조회
     */
    @Transactional(readOnly = true)
    public AdminAccountResponse getAdmin(Long adminId) {
        AdminAccount admin = adminAccountMapper.findById(adminId);
        if (admin == null) throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        return toResponse(admin);
    }

    /**
     * 관리자 생성 (MANAGER / OPERATOR만 가능)
     */
    @Transactional
    public AdminAccountResponse createAdmin(AdminCreateRequest request, Long createdBy) {

        // username 중복 확인
        if (adminAccountMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 사용 중인 아이디입니다.");
        }

        // roleId 조회
        Long roleId = adminAccountMapper.findRoleIdByCode(request.getRoleCode());
        if (roleId == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 역할입니다.");

        // 관리자 저장
        AdminAccount admin = AdminAccount.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .createdBy(createdBy)
                .build();

        adminAccountMapper.insertAdmin(admin);

        // 역할 매핑 INSERT
        adminAccountMapper.insertUserRole(admin.getAdminId(), roleId, createdBy);

        log.info("[AdminAccount] 관리자 생성: adminId={}, username={}, role={}",
                admin.getAdminId(), admin.getUsername(), request.getRoleCode());

        return toResponse(adminAccountMapper.findById(admin.getAdminId()));
    }

    /**
     * SUPER_ADMIN 생성 (별도 API)
     */
    @Transactional
    public AdminAccountResponse createSuperAdmin(AdminCreateRequest request, Long createdBy) {

        if (adminAccountMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 사용 중인 아이디입니다.");
        }

        Long roleId = adminAccountMapper.findRoleIdByCode("SUPER_ADMIN");
        if (roleId == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "SUPER_ADMIN 역할을 찾을 수 없습니다.");

        AdminAccount admin = AdminAccount.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .createdBy(createdBy)
                .build();

        adminAccountMapper.insertAdmin(admin);
        adminAccountMapper.insertUserRole(admin.getAdminId(), roleId, createdBy);

        log.info("[AdminAccount] SUPER_ADMIN 생성: adminId={}, username={}",
                admin.getAdminId(), admin.getUsername());

        return toResponse(adminAccountMapper.findById(admin.getAdminId()));
    }

    /**
     * 역할 변경
     */
    @Transactional
    public void updateRole(Long adminId, AdminRoleUpdateRequest request) {
        AdminAccount admin = adminAccountMapper.findById(adminId);
        if (admin == null) throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);

        // SUPER_ADMIN은 역할 변경 불가
        if ("SUPER_ADMIN".equals(admin.getRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "최상위 관리자의 역할은 변경할 수 없습니다.");
        }

        Long roleId = adminAccountMapper.findRoleIdByCode(request.getRoleCode());
        if (roleId == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 역할입니다.");

        adminAccountMapper.updateRole(adminId, roleId);
        log.info("[AdminAccount] 역할 변경: adminId={}, newRole={}", adminId, request.getRoleCode());
    }

    /**
     * 상태 변경 (ACTIVE / INACTIVE / LOCKED)
     */
    @Transactional
    public void updateStatus(Long adminId, String statusCode) {
        AdminAccount admin = adminAccountMapper.findById(adminId);
        if (admin == null) throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);

        if ("SUPER_ADMIN".equals(admin.getRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "최상위 관리자의 상태는 변경할 수 없습니다.");
        }

        adminAccountMapper.updateStatus(adminId, statusCode);
        log.info("[AdminAccount] 상태 변경: adminId={}, status={}", adminId, statusCode);
    }

    // ── private ──────────────────────────────────────────────────────

    private AdminAccountResponse toResponse(AdminAccount admin) {
        List<String> permissions = adminAccountMapper
                .findPermissionsByAdminId(admin.getAdminId());
        return AdminAccountResponse.builder()
                .adminId(admin.getAdminId())
                .username(admin.getUsername())
                .name(admin.getName())
                .email(admin.getEmail())
                .phone(admin.getPhone())
                .statusCode(admin.getStatusCode())
                .roleCode(admin.getRoleCode())
                .roleName(admin.getRoleName())
                .permissions(permissions)
                .createdAt(admin.getCreatedAt() != null
                        ? admin.getCreatedAt().toString() : null)
                .lastLoginAt(admin.getLastLoginAt() != null
                        ? admin.getLastLoginAt().toString() : null)
                .build();
    }
}