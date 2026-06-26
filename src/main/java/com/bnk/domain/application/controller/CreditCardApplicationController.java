package com.bnk.domain.application.controller;

import com.bnk.domain.application.dto.request.CreditCardApplicationRequest;
import com.bnk.domain.application.dto.request.ReviewResultRequest;
import com.bnk.domain.application.dto.request.ScreeningResultRequest;
import com.bnk.domain.application.dto.response.CreditApplicationResponse;
import com.bnk.domain.application.service.CreditCardApplicationService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.util.FileStorageService;
import com.bnk.global.util.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/applications/credit")
@RequiredArgsConstructor
public class CreditCardApplicationController {

    private final CreditCardApplicationService creditCardApplicationService;
    private final ObjectStorageService         objectStorageService;
    private final FileStorageService           fileStorageService;

    // ----------------------------------------------------------------
    // STEP 1 - 약관 동의
    // ----------------------------------------------------------------
    @PostMapping("/agree-terms")
    public ResponseEntity<ApiResponse<Long>> agreeTerms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreditCardApplicationRequest request) {

        Long creditAppId = creditCardApplicationService.agreeTerms(userDetails.getUserId(), request);
        return ApiResponse.toOk(creditAppId);
    }

    // ----------------------------------------------------------------
    // STEP 2 - 본인확인 결과 수신 (심사서버가 호출)
    // ----------------------------------------------------------------
    @PostMapping("/verify-identity")
    public ResponseEntity<ApiResponse<String>> verifyIdentity(
            @RequestBody CreditCardApplicationRequest request) {

        String idVerifiedYn = creditCardApplicationService.verifyIdentity(request);
        return ApiResponse.toOk(idVerifiedYn);
    }

    // ----------------------------------------------------------------
    // STEP 3 - 기본정보 + 직업/소득 저장
    // ----------------------------------------------------------------
    @PostMapping("/applicant-info")
    public ResponseEntity<ApiResponse<Void>> saveApplicantInfo(
            @RequestBody CreditCardApplicationRequest request) {

        creditCardApplicationService.saveApplicantInfo(request);
        return ApiResponse.toNoContent();
    }

    // ----------------------------------------------------------------
    // STEP 4 - 신청정보 저장 + 신청 완료(REQUESTED)
    // ----------------------------------------------------------------
    
    // 기존고객 여부 체크 (페이지 진입 시 서류 업로드 UI 표시 여부 결정)
    @GetMapping("/existing-customer")
    public ResponseEntity<ApiResponse<Boolean>> checkExistingCustomer(
            @RequestParam Long creditAppId) {
        boolean isExisting = creditCardApplicationService.checkExistingCustomer(creditAppId);
        return ApiResponse.toOk(isExisting);
    }

    //  서류 업로드 (신규고객만)
    @PostMapping("/docs")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocs(
            @RequestParam Long creditAppId,
            @RequestParam MultipartFile incomeDoc,
            @RequestParam(required = false) MultipartFile assetDoc,
            @RequestParam MultipartFile jobDoc) throws Exception {

        // 소득확인서류
        FileStorageService.UploadResult incomeMeta = fileStorageService.extractMeta(incomeDoc, "docs/income");
        String incomeDocKey = objectStorageService.upload(incomeMeta.getObjectName(), incomeDoc.getBytes(), incomeMeta.getMimeType());

        // 재산확인서류 (선택)
        String assetDocKey = null;
        if (assetDoc != null && !assetDoc.isEmpty()) {
            FileStorageService.UploadResult assetMeta = fileStorageService.extractMeta(assetDoc, "docs/asset");
            assetDocKey = objectStorageService.upload(assetMeta.getObjectName(), assetDoc.getBytes(), assetMeta.getMimeType());
        }

        // 직업확인서류
        FileStorageService.UploadResult jobMeta = fileStorageService.extractMeta(jobDoc, "docs/job");
        String jobDocKey = objectStorageService.upload(jobMeta.getObjectName(), jobDoc.getBytes(), jobMeta.getMimeType());

        log.info("[신용카드] 서류 업로드 완료: creditAppId={}", creditAppId);

        Map<String, String> response = new HashMap<>();
        response.put("incomeDocKey", incomeDocKey);
        response.put("assetDocKey", assetDocKey);
        response.put("jobDocKey", jobDocKey);

        return ResponseEntity.ok(ApiResponse.ok("서류 제출이 완료되었습니다.", response));
    }
    
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitApplication(
            @RequestBody CreditCardApplicationRequest request) {

        creditCardApplicationService.submitApplication(request);
        return ApiResponse.toNoContent();
    }
    

    // ----------------------------------------------------------------
    // STEP 6 - 1차 심사 결과 수신 (심사서버가 호출)
    // -----------------------------a-----------------------------------
    @PostMapping("/screening-result")
    public ResponseEntity<ApiResponse<Void>> saveScreeningResult(
            @RequestBody ScreeningResultRequest request) {

        creditCardApplicationService.saveScreeningResult(request);
        return ApiResponse.toNoContent();
    }

    // ----------------------------------------------------------------
    // STEP 7 - 한도 검증 : 6단계 서비스에서 자동
    // ----------------------------------------------------------------
//    @PostMapping("/limit-check")
//    public ResponseEntity<ApiResponse<Void>> checkLimit(
//            @RequestParam Long creditAppId) {
//
//        creditCardApplicationService.checkLimit(creditAppId);
//        return ApiResponse.toNoContent();
//    }

    // ----------------------------------------------------------------
    // STEP 8 - 추가 심사 결과 수신 (심사서버가 호출)
    // ----------------------------------------------------------------
    @PostMapping("/review-result")
    public ResponseEntity<ApiResponse<Void>> saveReviewResult(
            @RequestBody ReviewResultRequest request) {

        creditCardApplicationService.saveReviewResult(request);
        return ApiResponse.toNoContent();
    }
    

    // ----------------------------------------------------------------
    // 사용자 조회
    // ---------------------------------------------------------------- 
    @GetMapping("/{creditAppId}")
    public ResponseEntity<ApiResponse<CreditApplicationResponse>> getApplication(
            @PathVariable Long creditAppId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResponse.toOk(creditCardApplicationService.findOne(creditAppId, userDetails.getUserId()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CreditApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResponse.toOk(creditCardApplicationService.findMyApplications(userDetails.getUserId()));
    }    
    

    // ----------------------------------------------------------------
    // 임시 저장
    // ---------------------------------------------------------------- 
    @GetMapping("/draft")
    public ResponseEntity<ApiResponse<CreditApplicationResponse>> getDraftApplication(
            @RequestParam Long cardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.toOk(
            creditCardApplicationService.findDraftByCardId(cardId, userDetails.getUserId())
        );
    }
    
}