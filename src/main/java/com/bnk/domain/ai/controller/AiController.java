package com.bnk.domain.ai.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.ai.dto.AiChatRequest;
import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.domain.ai.service.AiChatService;
import com.bnk.domain.ai.service.CardVectorService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.ai.google.genai.enabled", havingValue = "true")
public class AiController {
	
	private final AiChatService aiChatService;
	
	private final CardVectorService cardVectorService;

    @PostMapping("/init")
    public ResponseEntity<String> initializeVectors() {
        log.info("관리자 요청: Qdrant 카드 데이터 인덱싱 시작...");
        
        cardVectorService.indexAllCards(); 
        
        return ResponseEntity.ok("카드 데이터 인덱싱이 시작되었습니다.");
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
        return ApiResponse.toOk(aiChatService.chat(request, userId));
    }
}
