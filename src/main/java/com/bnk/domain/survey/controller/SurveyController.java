package com.bnk.domain.survey.controller;

import com.bnk.domain.survey.dto.request.SurveySubmitRequest;
import com.bnk.domain.survey.dto.response.SurveyResultResponse;
import com.bnk.domain.survey.dto.response.SurveyStatusResponse;
import com.bnk.domain.survey.service.SurveyService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    /**
     * 이번 달 설문 완료 여부 확인
     * GET /api/surveys/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SurveyStatusResponse>> getStatus(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(
                surveyService.getStatus(ud.getUserId()));
    }

    /**
     * 설문 제출
     * POST /api/surveys
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SurveyResultResponse>> submit(
            @RequestBody @Valid SurveySubmitRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toCreated(
                surveyService.submit(request, ud.getUserId()));
    }
}