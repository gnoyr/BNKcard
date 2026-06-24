package com.bnk.domain.spending.model;

import lombok.*;
import java.time.LocalDate;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SpendingPattern {
    private Long patternId;
    private Long userId;
    private Long categoryId;
    private Long monthlyAmount;
    private String source;          // MANUAL / AUTO
    private LocalDate updatedAt;
    // CARD_CATEGORIES JOIN
    private String categoryName;
    private String iconCode;
}
