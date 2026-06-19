package com.bnk.domain.card.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardTerms {
    private Long cardTermsId;
    private Long cardId;
    private Long termsId;
    private Long groupId;
    private String requiredYn;
    private String exposureConditionJson;
    private Integer displayOrder;
    private LocalDateTime createdAt;

    // JOIN 결과 (TERMS, TERMS_MASTERS)
    private String title;
    private String version;
    private String contentHtml;
}