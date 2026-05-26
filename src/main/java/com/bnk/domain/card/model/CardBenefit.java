package com.bnk.domain.card.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBenefit {
    private Long benefitId;
    private Long cardId;
    private Long categoryId;
    private String benefitTitle;
    private String benefitType;         // RATE_DISCOUNT/FIXED_DISCOUNT/POINT/CASHBACK/FREE
    private BigDecimal discountRate;    // 10% → 0.1000
    private Long discountAmount;
    private BigDecimal pointRate;
    private BigDecimal cashbackRate;
    private Long monthlyLimitAmount;
    private Long dailyLimitAmount;
    private Long minimumPaymentAmount;
    private String displayText;
    private Integer displayOrder;
    private String visibleYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String benefitCondition;
    // JOIN 필드
    private String categoryName;
    private String iconCode;
}
