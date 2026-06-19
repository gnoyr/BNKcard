package com.bnk.domain.admin.service;

import com.bnk.domain.admin.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    /**
     * 역할에 따라 다른 대시보드 데이터 반환
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(Long adminId, String roleCode) {
        return switch (roleCode) {
            case "SUPER_ADMIN" -> buildSuperAdminStats();
            case "MANAGER"     -> buildManagerStats(adminId);
            case "OPERATOR"    -> buildOperatorStats(adminId);
            default            -> Map.of();
        };
    }

    // ── SUPER_ADMIN ──────────────────────────────────────────────────

    private Map<String, Object> buildSuperAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        // 카드 상태별 분포
        stats.put("cardsByStatus",    dashboardMapper.countCardsByStatus());

        // 관리자 역할별 분포
        stats.put("adminsByRole",     dashboardMapper.countAdminsByRole());

        // 최근 7일 결재 처리 건수
        stats.put("approvalsByDay",   dashboardMapper.countApprovalsByDay());

        // 최근 7일 회원 가입 추이
        stats.put("signupByDay",      dashboardMapper.countUserSignupByDay());

        // 회원 요약
        stats.put("totalUsers",       dashboardMapper.countTotalUsers());
        stats.put("todaySignup",      dashboardMapper.countTodaySignup());
        stats.put("suspendedUsers",   dashboardMapper.countSuspendedUsers());

        // 최근 관리자 활동
        stats.put("recentAdminActivity", dashboardMapper.findRecentAdminActivity());

        log.debug("[Dashboard] SUPER_ADMIN stats 조회 완료");
        return stats;
    }

    // ── MANAGER ──────────────────────────────────────────────────────

    private Map<String, Object> buildManagerStats(Long adminId) {
        Map<String, Object> stats = new HashMap<>();

        // 카드 상태별 분포
        stats.put("cardsByStatus",      dashboardMapper.countCardsByStatus());

        // 약관 상태별 분포
        stats.put("termsByStatus",      dashboardMapper.countTermsByStatus());

        // 대기중 결재 목록
        stats.put("pendingApprovals",   dashboardMapper.findPendingApprovals());

        // 이번달 내 결재 처리 현황
        stats.put("myApprovalsByDay",   dashboardMapper.countMyApprovalsByDay(adminId));

        // 내 최근 활동
        stats.put("myRecentActivity",   dashboardMapper.findMyRecentActivity(adminId));

        log.debug("[Dashboard] MANAGER stats 조회 완료: adminId={}", adminId);
        return stats;
    }

    // ── OPERATOR ─────────────────────────────────────────────────────

    private Map<String, Object> buildOperatorStats(Long adminId) {
        Map<String, Object> stats = new HashMap<>();

        // 회원 상태별 분포
        stats.put("usersByStatus",    dashboardMapper.countUsersByStatus());

        // 회원 요약 수치
        stats.put("totalUsers",       dashboardMapper.countTotalUsers());
        stats.put("todaySignup",      dashboardMapper.countTodaySignup());
        stats.put("suspendedUsers",   dashboardMapper.countSuspendedUsers());

        // 최근 7일 로그인 추이
        stats.put("loginByDay",       dashboardMapper.countLoginByDay());

        // 오늘 검색 키워드 TOP 5
        stats.put("topKeywords",      dashboardMapper.findTopKeywordsToday());

        // 잠금 회원 목록
        stats.put("lockedUsers",      dashboardMapper.findLockedUsers());

        // 내 최근 활동
        stats.put("myRecentActivity", dashboardMapper.findMyRecentActivity(adminId));

        log.debug("[Dashboard] OPERATOR stats 조회 완료: adminId={}", adminId);
        return stats;
    }
}