package com.bnk.domain.terms.controller;

import com.bnk.domain.admin.service.AdminTermsService;
import com.bnk.domain.terms.dto.request.TermsCreateRequest;
import com.bnk.domain.terms.dto.request.TermsStatusRequest;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/terms")
@RequiredArgsConstructor
public class AdminTermsController {

    private final AdminTermsService adminTermsService;

    /**
     * B-11 약관 신규 버전 등록 + PDF 변환 (RQ-B11, B-13).
     * TERMS INSERT(DRAFT) + TERMS_FILES INSERT(PDF 원본 + JPG 변환본).
     * multipart/form-data: JSON 파트(request) + PDF 파트(pdfFile).
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Void>> registerTermsWithPdf(
            @RequestPart("request") @Valid TermsCreateRequest request,
            @RequestPart("pdfFile") MultipartFile pdfFile,
            @AuthenticationPrincipal CustomAdminDetails ad) throws IOException {
        adminTermsService.registerTermsWithPdf(request, pdfFile);
        return ApiResponse.toOk(null);
    }

    /**
     * B-12 약관 상태 변경 (RQ-B12).
     * DRAFT → REVIEW → APPROVED → PUBLISHED 순서로 전환.
     * PUBLISHED 전환 시 reconsent_required_yn='Y' 이면 알림 발송.
     * TERMS_STATUS_HISTORY INSERT 필수.
     */
    @PatchMapping("/{termsId}/status")
    public ResponseEntity<ApiResponse<Void>> changeTermsStatus(
            @PathVariable Long termsId,
            @RequestBody @Valid TermsStatusRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        adminTermsService.changeTermsStatus(termsId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }
}