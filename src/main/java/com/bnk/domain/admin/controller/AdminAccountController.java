package com.bnk.domain.admin.controller;

import com.bnk.domain.admin.dto.request.AdminCreateRequest;
import com.bnk.domain.admin.dto.request.AdminRoleUpdateRequest;
import com.bnk.domain.admin.dto.response.AdminAccountResponse;
import com.bnk.domain.admin.service.AdminAccountService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    /**
     * 관리자 목록 조회
     * GET /api/admin/admins
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminAccountResponse>>> getAdminList() {
        return ApiResponse.toOk(adminAccountService.getAdminList());
    }

    /**
     * 관리자 단건 조회
     * GET /api/admin/admins/{adminId}
     */
    @GetMapping("/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminAccountResponse>> getAdmin(
            @PathVariable Long adminId) {
        return ApiResponse.toOk(adminAccountService.getAdmin(adminId));
    }

    /**
     * 관리자 생성 (MANAGER / OPERATOR)
     * POST /api/admin/admins
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminAccountResponse>> createAdmin(
            @RequestBody @Valid AdminCreateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toCreated(
                adminAccountService.createAdmin(request, ad.getAdminId()));
    }

    /**
     * SUPER_ADMIN 생성 (별도 엔드포인트)
     * POST /api/admin/admins/super
     */
    @PostMapping("/super")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminAccountResponse>> createSuperAdmin(
            @RequestBody @Valid AdminCreateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toCreated(
                adminAccountService.createSuperAdmin(request, ad.getAdminId()));
    }

    /**
     * 역할 변경
     * PUT /api/admin/admins/{adminId}/role
     */
    @PutMapping("/{adminId}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable Long adminId,
            @RequestBody @Valid AdminRoleUpdateRequest request) {
        adminAccountService.updateRole(adminId, request);
        return ApiResponse.toOk(null);
    }

    /**
     * 상태 변경 (ACTIVE / INACTIVE / LOCKED)
     * PATCH /api/admin/admins/{adminId}/status
     */
    @PatchMapping("/{adminId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long adminId,
            @RequestBody Map<String, String> body) {
        String statusCode = body.get("statusCode");
        adminAccountService.updateStatus(adminId, statusCode);
        return ApiResponse.toOk(null);
    }
}