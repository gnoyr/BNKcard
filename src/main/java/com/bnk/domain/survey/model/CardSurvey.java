package com.bnk.domain.survey.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSurvey {
    private Long          surveyId;
    private Long          userId;
    private LocalDateTime submittedAt;
    private String        rewardGivenYn;
}