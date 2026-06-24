package com.bnk.domain.admin.controller;

import com.bnk.domain.admin.dto.request.ApprovalActionRequest;
import com.bnk.domain.admin.dto.request.ApprovalSearchRequest;
import com.bnk.domain.admin.dto.response.ApprovalDetailResponse;
import com.bnk.domain.admin.dto.response.ApprovalListResponse;
import com.bnk.domain.admin.service.ApprovalService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /** 결재 목록 조회 — 현재 로그인 관리자 할당 건만 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApprovalListResponse>>> getApprovals(
            @ModelAttribute ApprovalSearchRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {  // ← 추가
        return ApiResponse.toOk(approvalService.getApprovals(request, ad.getAdminId())); // ← adminId 추가
    }

    /** 결재 상세 조회 */
    @GetMapping("/{approvalId}")
    public ResponseEntity<ApiResponse<ApprovalDetailResponse>> getApprovalDetail(
    		@PathVariable Long approvalId,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(approvalService.getApprovalDetail(approvalId, ad.getAdminId()));
    }

    /** 결재 승인 */
    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRequest(
    		@PathVariable Long approvalId,
            @RequestBody @Valid ApprovalActionRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        approvalService.approve(approvalId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }

    /** 결재 반려 */
    @PostMapping("/{approvalId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
    		@PathVariable Long approvalId,
            @RequestBody @Valid ApprovalActionRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        approvalService.reject(approvalId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }
}