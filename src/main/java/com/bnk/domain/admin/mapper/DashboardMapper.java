package com.bnk.domain.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    // ── SUPER_ADMIN ──────────────────────────────────────────────────

    /** 카드 상태별 수 */
    List<Map<String, Object>> countCardsByStatus();

    /** 관리자 역할별 수 */
    List<Map<String, Object>> countAdminsByRole();

    /** 최근 7일 결재 처리 건수 (일별 승인/반려) */
    List<Map<String, Object>> countApprovalsByDay();

    /** 최근 7일 회원 가입 추이 */
    List<Map<String, Object>> countUserSignupByDay();

    /** 전체 회원 수 */
    long countTotalUsers();

    /** 오늘 가입자 수 */
    long countTodaySignup();

    /** 정지 계정 수 */
    long countSuspendedUsers();

    /** 최근 관리자 활동 로그 */
    List<Map<String, Object>> findRecentAdminActivity();

    // ── MANAGER ──────────────────────────────────────────────────────

    /** 대기중 결재 목록 */
    List<Map<String, Object>> findPendingApprovals();

    /** 이번달 내 결재 처리 현황 (일별) */
    List<Map<String, Object>> countMyApprovalsByDay(@Param("adminId") Long adminId);

    /** 약관 상태별 수 */
    List<Map<String, Object>> countTermsByStatus();

    /** 내 최근 활동 */
    List<Map<String, Object>> findMyRecentActivity(@Param("adminId") Long adminId);

    // ── OPERATOR ─────────────────────────────────────────────────────

    /** 최근 7일 로그인 성공/실패 추이 */
    List<Map<String, Object>> countLoginByDay();

    /** 오늘 검색 키워드 TOP 5 */
    List<Map<String, Object>> findTopKeywordsToday();

    /** 회원 상태별 분포 */
    List<Map<String, Object>> countUsersByStatus();

    /** 잠금 회원 목록 */
    List<Map<String, Object>> findLockedUsers();
}