package com.bnk.domain.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.ai.dto.AiChatRequest;
import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.domain.ai.service.AiChatService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {
	
	private final AiChatService aiChatService;
	
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
        return ApiResponse.toOk(aiChatService.chat(request, userId));
    }
}
