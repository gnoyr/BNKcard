package com.bnk.domain.card.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BenefitDto {
    private Long benefitId;
    private String benefitTitle;
    private String benefitType;
    private BigDecimal discountRate;
    private BigDecimal cashbackRate;
    private Long monthlyLimitAmount;
    private String displayText;
    private Integer displayOrder;
    private String categoryName;
    private String iconCode;
    private Long estimatedBenefit;      // 시뮬레이션 계산값 (monthlySpend * rate)
}
