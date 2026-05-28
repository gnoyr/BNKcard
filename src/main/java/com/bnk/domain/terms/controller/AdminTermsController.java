package com.bnk.domain.terms.controller;

import com.bnk.domain.admin.service.AdminTermsService;
import com.bnk.domain.terms.dto.request.TermsCreateRequest;
import com.bnk.domain.terms.dto.request.TermsStatusRequest;
import com.bnk.domain.terms.dto.response.TermsAdminResponse;
import com.bnk.domain.terms.dto.response.TermsMasterResponse;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/terms")
@RequiredArgsConstructor
public class AdminTermsController {

    private final AdminTermsService adminTermsService;

    /**
     * 약관 신규 등록 + 결재 신청 (수정 후)
     * POST /api/admin/terms
     * 반환: { termsId, approvalId }
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, Long>>> registerTerms(
            @RequestPart("request") @Valid TermsCreateRequest request,
            @RequestPart("pdfFile") MultipartFile pdfFile,
            @AuthenticationPrincipal CustomAdminDetails ad) throws IOException {
        Map<String, Long> result =
            adminTermsService.registerTermsWithApproval(request, pdfFile, ad.getAdminId());
        return ApiResponse.toCreated(result);
    }

    /**
     * 약관 상태 직접 변경 (긴급용 — 정상 흐름은 Approval)
     * PATCH /api/admin/terms/{termsId}/status
     */
    @PatchMapping("/{termsId}/status")
    public ResponseEntity<ApiResponse<Void>> changeTermsStatus(
            @PathVariable Long termsId,
            @RequestBody @Valid TermsStatusRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        adminTermsService.changeTermsStatus(termsId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }

    @PostMapping(value = "/{termsId}/files", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Void>> addTermsFile(
            @PathVariable Long termsId,
            @RequestPart("pdfFile") MultipartFile pdfFile,
            @AuthenticationPrincipal CustomAdminDetails ad) throws IOException {
        adminTermsService.addFileToExistingTerms(termsId, pdfFile);
        return ApiResponse.toOk(null);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TermsAdminResponse>>> getTermsList(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminTermsService.getTermsList(status));
    }

    @GetMapping("/{termsId}")
    public ResponseEntity<ApiResponse<TermsAdminResponse>> getTermsDetail(
            @PathVariable Long termsId,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminTermsService.getTermsDetail(termsId));
    }

    @GetMapping("/masters")
    public ResponseEntity<ApiResponse<List<TermsMasterResponse>>> getTermsMasters(
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminTermsService.getTermsMasters());
    }
}