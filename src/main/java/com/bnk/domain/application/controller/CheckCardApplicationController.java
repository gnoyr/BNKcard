package com.bnk.domain.application.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.application.dto.request.CheckCardApplicationRequest;
import com.bnk.domain.application.dto.request.ScreeningResultRequest;
import com.bnk.domain.application.dto.response.CheckApplicationResponse;
import com.bnk.domain.application.service.CheckCardApplicationService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.util.InternalCallbackValidator;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/applications/check")
@RequiredArgsConstructor
public class CheckCardApplicationController {
	
	private final CheckCardApplicationService checkCardApplicationService;
	private final InternalCallbackValidator   internalCallbackValidator;
	
	// ----------------------------------------------------------------
    // STEP 1 - 약관 동의
    // ----------------------------------------------------------------
    @PostMapping("/agree-terms")
    public ResponseEntity<ApiResponse<Long>> agreeTerms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CheckCardApplicationRequest request) {

        Long checkAppId = checkCardApplicationService.agreeTerms(userDetails.getUserId(), request);
        return ApiResponse.toOk(checkAppId);
    }

    // ----------------------------------------------------------------
    // STEP 2 - 본인확인 (사용자가 직접 호출)
    // ----------------------------------------------------------------
    @PostMapping("/verify-identity")
    public ResponseEntity<ApiResponse<String>> verifyIdentity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CheckCardApplicationRequest request) {

        checkCardApplicationService.validateOwnership(request.getCheckAppId(), userDetails.getUserId());
        String idVerifiedYn = checkCardApplicationService.verifyIdentity(request);
        return ApiResponse.toOk(idVerifiedYn);
    }

    // ----------------------------------------------------------------
    // STEP 3 - 기본정보
    // ----------------------------------------------------------------
    @PostMapping("/applicant-info")
    public ResponseEntity<ApiResponse<Void>> saveApplicantInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CheckCardApplicationRequest request) {

        checkCardApplicationService.validateOwnership(request.getCheckAppId(), userDetails.getUserId());
        checkCardApplicationService.saveApplicantInfo(request);
        return ApiResponse.toNoContent();
    }

    // ----------------------------------------------------------------
    // STEP 4 - + 신청정보 저장 + 신청 완료(REQUESTED)
    // ----------------------------------------------------------------
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitApplication(
            @RequestBody CheckCardApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        checkCardApplicationService.submitApplication(request, userDetails.getUserId());
        return ApiResponse.toNoContent();
    }

    // ----------------------------------------------------------------
    // STEP 5 - 심사 결과 수신 (심사서버가 호출 — X-Internal-Secret 필요)
    // ----------------------------------------------------------------
    @PostMapping("/screening-result")
    public ResponseEntity<ApiResponse<Void>> saveScreeningResult(
            HttpServletRequest httpRequest,
            @RequestBody ScreeningResultRequest request) {

        internalCallbackValidator.validate(httpRequest);
        checkCardApplicationService.saveScreeningResult(request);
        return ApiResponse.toNoContent();
    }
    
    
    // ----------------------------------------------------------------
    // Fix 4: SCREENING_FAILED 심사 재시도
    // ----------------------------------------------------------------
    @PostMapping("/{checkAppId}/retry-screening")
    public ResponseEntity<ApiResponse<Void>> retryScreening(
            @PathVariable Long checkAppId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkCardApplicationService.retryScreening(checkAppId, userDetails.getUserId());
        return ApiResponse.toNoContent();
    }

    // ----------------------------------------------------------------
    // 사용자 조회
    // ----------------------------------------------------------------
    @GetMapping("/{checkAppId}")
    public ResponseEntity<ApiResponse<CheckApplicationResponse>> getApplication(
            @PathVariable Long checkAppId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResponse.toOk(checkCardApplicationService.findOne(checkAppId, userDetails.getUserId()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CheckApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResponse.toOk(checkCardApplicationService.findMyApplications(userDetails.getUserId()));
    }
    
    // ----------------------------------------------------------------
    // 임시 저장
    // ---------------------------------------------------------------- 
    @GetMapping("/draft")
    public ResponseEntity<ApiResponse<CheckApplicationResponse>> getDraftApplication(
            @RequestParam Long cardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.toOk(
            checkCardApplicationService.findDraftByCardId(cardId, userDetails.getUserId())
        );
    }
}