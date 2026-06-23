package com.bnk.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlySpendingResponse {

    /** 이번달 총 이용금액 */
    private long totalAmount;

    /** 카드별 이용금액 목록 */
    private List<CardSpending> cards;

    @Getter
    @Builder
    public static class CardSpending {
        private Long   userCardId;
        private String cardName;
        private long   amount;
    }
}