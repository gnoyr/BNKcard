package com.bnk.domain.spending.controller;

import com.bnk.domain.spending.dto.request.AiChatRequest;
import com.bnk.domain.spending.dto.request.SpendingPatternRequest;
import com.bnk.domain.spending.dto.response.AiChatResponse;
import com.bnk.domain.spending.dto.response.SpendingChartResponse;
import com.bnk.domain.spending.service.SpendingService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SpendingController {

    private final SpendingService spendingService;

    /**
     * 소비패턴 조회 (차트 데이터).
     * category별 monthly_amount + percentage(%) 반환.
     */
    @GetMapping("/users/me/spending")
    public ResponseEntity<ApiResponse<List<SpendingChartResponse>>> getMySpendingPatterns(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(spendingService.getMySpendingPatterns(ud.getUserId()));
    }

    /**
     * 소비패턴 수동 입력/수정.
     * MERGE INTO (user_id + category_id 기준 UPSERT). source='MANUAL'.
     */
    @PutMapping("/users/me/spending")
    public ResponseEntity<ApiResponse<Integer>> updateSpendingPatterns(
            @RequestBody @Valid SpendingPatternRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        int updatedCount = spendingService.updateSpendingPatterns(ud.getUserId(), request);
        return ApiResponse.toOk(updatedCount);
    }

    /**
     * AI 챗봇 대화.
     * 비로그인 허용 — ud=null 이면 userId=null로 AI_CHAT_LOGS INSERT.
     * sessionId(UUID)로 대화 맥락 유지.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestBody @Valid AiChatRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        Long userId = ud != null ? ud.getUserId() : null;
        return ApiResponse.toOk(spendingService.chat(request, userId));
    }
}
