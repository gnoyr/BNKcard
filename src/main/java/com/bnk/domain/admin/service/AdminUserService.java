package com.bnk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.admin.dto.request.AdminUserSearchRequest;
import com.bnk.domain.admin.dto.response.AdminUserResponse;
import com.bnk.domain.admin.dto.response.DashboardResponse;
import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;

    public DashboardResponse getDashboard() {
        // TODO Auto-generated method stub
        return null;
    }

    public AdminUserResponse getUserDetail(Long userId, Long adminId) {
        // TODO Auto-generated method stub
        return null;
    }

    public PageResponse<AdminUserResponse> getUserList(AdminUserSearchRequest request, Long adminId) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * [개선 #2] 관리자 → 일반 유저 계정 잠금 강제 해제.
     * USERS.locked_until = NULL, login_fail_count = 0 처리.
     * AUDIT_LOGS에 해제 이력을 기록한다.
     *
     * @param userId  잠금 해제 대상 유저 ID
     * @param adminId 처리 관리자 ID (감사 로그용)
     */
    @Transactional
    public void unlockUser(Long userId, Long adminId) {

        int affected = adminUserMapper.unlockUser(userId);

        if (affected == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND,
                    "잠금 해제 대상 유저를 찾을 수 없습니다. userId=" + userId);
        }

        // 감사 로그 기록
        adminUserMapper.insertAuditLog(
                "ADMIN", adminId,
                "UNLOCK_USER",
                "USER", userId,
                "계정 잠금 강제 해제",
                null
        );

        log.info("[계정잠금해제] adminId={} → userId={}", adminId, userId);
    }
}