package com.bnk.domain.card.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SimulationResponse {
    private Long cardId;
    private String cardName;
    private Long totalBenefitAmount;        // 카드별 예상 혜택 합계
    private List<BenefitBreakdown> benefitBreakdown;

    @Getter
    @Builder
    public static class BenefitBreakdown {
        private String categoryName;
        private Long benefitAmount;         // MIN(월지출 * rate, monthlyLimit)
    }
}
