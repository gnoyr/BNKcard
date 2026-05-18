package com.bnk.domain.spending.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiChatRequest {

    @NotBlank(message = "세션 ID는 필수입니다.")
    private String sessionId;   // UUID — 대화 맥락 유지

    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 2000, message = "메시지는 2000자 이내로 입력해주세요.")
    private String userInput;
}
