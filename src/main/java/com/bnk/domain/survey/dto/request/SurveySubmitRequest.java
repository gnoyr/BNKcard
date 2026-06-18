package com.bnk.domain.survey.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class SurveySubmitRequest {

    @NotEmpty(message = "설문 응답은 필수입니다.")
    private List<AnswerItem> answers;

    private String commentText; // 자유 의견 (선택)

    @Getter
    @NoArgsConstructor
    public static class AnswerItem {
        private String questionCode; // Q1, Q2, Q3...
        private String answerValue;  // 선택값 (복수 선택 시 콤마 구분)
    }
}