package com.bnk.domain.survey.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SurveyResultResponse {
    private Long    surveyId;
    private boolean rewardGiven;
    private String  message;
}