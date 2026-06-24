package com.bnk.domain.terms.controller;

import com.bnk.domain.terms.dto.request.TermsAgreementRequest;
import com.bnk.domain.terms.dto.response.TermsFileResponse;
import com.bnk.domain.terms.dto.response.TermsPackageResponse;
import com.bnk.domain.terms.service.TermsService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    /**
     * 약관 패키지 조회 (SIGNUP / CARD_APPLY).
     * PACKAGE_TERMS → TERMS(status=PUBLISHED) → TERMS_MASTERS JOIN.
     * 비로그인 허용 (회원가입 화면에서 호출).
     */
    @GetMapping("/packages/{packageType}")
    public ResponseEntity<ApiResponse<TermsPackageResponse>> getTermsPackage(
            @PathVariable String packageType) {
        return ApiResponse.toOk(termsService.getTermsPackage(packageType));
    }

    /**
     * 약관 동의 처리.
     * required_yn='Y' 전체 동의 검증 → USER_TERMS_AGREEMENTS 배치 INSERT.
     * agreedContentSnapshot(CLOB) 법적 증거 저장.
     */
    @PostMapping("/agree")
    public ResponseEntity<ApiResponse<List<Long>>> agreeTerms(
            @RequestBody @Valid TermsAgreementRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toCreated(termsService.agreeTerms(request, ud.getUserId()));
    }
    
    /**
     * 약관 파일 URL 조회 (PDF 다운로드용).
     * TERMS_FILES에서 is_primary='Y'인 PDF URL 반환.
     */
    @GetMapping("/{termsId}/files")
    public ResponseEntity<ApiResponse<List<TermsFileResponse>>> getTermsFiles(
            @PathVariable Long termsId) {
        return ApiResponse.toOk(termsService.getTermsFiles(termsId));
    }
    
}
