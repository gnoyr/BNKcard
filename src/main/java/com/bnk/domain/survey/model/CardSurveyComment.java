package com.bnk.domain.survey.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSurveyComment {
    private Long          commentId;
    private Long          surveyId;
    private Long          userId;
    private String        commentText;
    private String        sentiment;      // POSITIVE / NEGATIVE / NEUTRAL
    private Double        sentimentScore; // -1.0 ~ 1.0
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
}