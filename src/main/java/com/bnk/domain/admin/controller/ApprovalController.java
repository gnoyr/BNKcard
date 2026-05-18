package com.bnk.domain.admin.controller;

import com.bnk.domain.admin.dto.request.ApprovalActionRequest;
import com.bnk.domain.admin.dto.request.ApprovalSearchRequest;
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

    /** 결재 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApprovalListResponse>>> getApprovals(
            @ModelAttribute ApprovalSearchRequest request) {
        return ApiResponse.toOk(approvalService.getApprovals(request));
    }

    /**
     * 결재 승인 (RQ-B07).
     * @Transactional: APPROVAL_LINES → APPROVAL_REQUESTS → CARD_VERSIONS.snapshot_json 역직렬화
     * → CARDS UPDATE → CARDS.card_status='PUBLISHED'
     */
    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRequest(
            @PathVariable Long approvalId,
            @RequestBody @Valid ApprovalActionRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        approvalService.approve(approvalId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }

    /**
     * 결재 반려 (RQ-B08).
     * comment 필수 검증은 서비스 단에서 처리 (REJECT_COMMENT_REQUIRED).
     * CARDS.card_status 변경 없음 — DRAFT 유지.
     */
    @PostMapping("/{approvalId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @PathVariable Long approvalId,
            @RequestBody @Valid ApprovalActionRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        approvalService.reject(approvalId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }
}
