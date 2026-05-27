package com.bnk.domain.admin.controller;

import com.bnk.domain.admin.dto.request.AdminUserSearchRequest;
import com.bnk.domain.admin.dto.response.AdminUserResponse;
import com.bnk.domain.admin.dto.response.DashboardResponse;
import com.bnk.domain.admin.service.AdminUserService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.response.PageResponse;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 관리자 대시보드 (RQ-B02, RQ-B03).
     * pendingApprovalCount / topCards(view_count DESC 10) / recentAdminLogins
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ApiResponse.toOk(adminUserService.getDashboard());
    }
    
    /**
     * 관리자 회원 목록 검색 (RQ-B14).
     * name·email·phone LIKE + statusCode= + birthDate BETWEEN 동적 조합.
     * phone·email 마스킹 필수. AUDIT_LOGS INSERT.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUserList(
            @ModelAttribute AdminUserSearchRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminUserService.getUserList(request, ad.getAdminId()));
    }

    /**
     * 관리자 회원 상세 조회 (RQ-B15).
     * 기본정보 + 로그인이력(5건) + 약관동의 + 신청이력.
     * AUDIT_LOGS INSERT (admin_id 기록). 개인정보 마스킹 필수.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserDetail(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminUserService.getUserDetail(userId, ad.getAdminId()));
    }

    /**
     * [개선 #2] 유저 계정 잠금 강제 해제.
     * USERS.locked_until = NULL, login_fail_count = 0.
     * AUDIT_LOGS INSERT.
     */
    @PatchMapping("/users/{userId}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        adminUserService.unlockUser(userId, ad.getAdminId());
        return ApiResponse.toOk(null);
    }
    
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> changeUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        adminUserService.changeUserStatus(userId, body.get("statusCode"), ad.getAdminId());
        return ApiResponse.toOk(null);
    }
}
