package com.bnk.domain.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiChatHistoryResponse {
    private Long chatId;
    private String sessionId;
    private String userInput;
    private String aiResponse;
}
