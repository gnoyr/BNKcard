package com.bnk.domain.card.dto.request2;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BenefitCreateRequest {
	
	private Long       categoryId;          // CARD_CATEGORIES 참조
    private String     benefitTitle;
    private String     benefitType;         // RATE_DISCOUNT / FIXED_DISCOUNT / POINT / CASHBACK / FREE
    private BigDecimal discountRate;        // NUMBER(5,4) — RATE_DISCOUNT 사용
    private Long       discountAmount;      // FIXED_DISCOUNT 사용
    private BigDecimal pointRate;           // NUMBER(5,4) — POINT 사용
    private BigDecimal cashbackRate;        // NUMBER(5,4) — CASHBACK 사용
    private Long       monthlyLimitAmount;  // null = 무제한
    private Long       dailyLimitAmount;
    private Long       minimumPaymentAmount;
    private String     benefitCondition;    // CLOB
    private String     displayText;
    private Integer    displayOrder;
    private String     visibleYn;
    
}
