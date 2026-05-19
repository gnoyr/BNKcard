package com.bnk.domain.ai.model;

import lombok.*;
import java.time.LocalDate;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class AiChatLog {
    private Long chatId;
    private Long userId;        // nullable — 비로그인 허용
    private String sessionId;   // UUID
    private String userInput;
    private String aiResponse;
    private LocalDate createdAt;
}
