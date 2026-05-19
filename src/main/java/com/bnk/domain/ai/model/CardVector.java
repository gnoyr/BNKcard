package com.bnk.domain.ai.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CardVector {
	private Long cardId;
    private String cardName;
    private String cardType;
    private String companyName;
    private String brandName;
    private Integer annualFeeDomestic;
    private Integer annualFeeOverseas;
    private Long previousMonthSpend;
    private String targetUser;
    private String summaryDescription;
    private List<BenefitVector> benefits;
    
    @Data
    public static class BenefitVector {
        private String benefitTitle;
        private String benefitType;
        private BigDecimal discountRate;
        private BigDecimal cashbackRate;
        private BigDecimal pointRate;
        private String displayText;
    }
    
}
