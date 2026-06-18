package com.bnk.domain.admin.controller;

import com.bnk.domain.admin.service.DashboardService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 대시보드 통계 조회
     * GET /api/admin/dashboard/stats
     * — 로그인한 관리자 역할에 따라 다른 데이터 반환
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @AuthenticationPrincipal CustomAdminDetails ad) {

        // 역할 추출 — ROLE_SUPER_ADMIN → SUPER_ADMIN
        String roleCode = ad.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("OPERATOR");

        Map<String, Object> stats = dashboardService.getDashboardStats(
                ad.getAdminId(), roleCode);

        return ApiResponse.toOk(stats);
    }
}