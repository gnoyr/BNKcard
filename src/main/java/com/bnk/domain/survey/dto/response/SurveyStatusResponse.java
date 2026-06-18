package com.bnk.domain.survey.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SurveyStatusResponse {
    private boolean completed;   // 이번 달 이미 완료했는지
    private String  lastSurveyAt; // 마지막 제출 일시
}