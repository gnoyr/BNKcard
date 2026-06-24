package com.bnk.domain.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiChatResponse {
    private Long chatId;
    private String sessionId;
    private String response;            // AI 응답 텍스트
}
