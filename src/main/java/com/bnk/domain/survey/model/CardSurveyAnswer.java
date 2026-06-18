package com.bnk.domain.survey.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSurveyAnswer {
    private Long   answerId;
    private Long   surveyId;
    private String questionCode;
    private String answerValue;
}