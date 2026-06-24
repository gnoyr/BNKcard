package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 혜택 시뮬레이션 요청 DTO — POST /api/cards/simulate
 * MIN(monthlyAmount * discountRate, monthlyLimitAmount) 공식 기반.
 */
@Getter
@NoArgsConstructor
public class CardSimulationRequest {

    @NotNull(message = "카드 ID 목록은 필수입니다.")
    @Size(min = 1, max = 3, message = "카드는 1~3개를 선택해주세요.")
    private List<Long> cardIds;

    /**
     * 카테고리별 월 지출액 (categoryId → 원 단위 금액)
     * 예: { 1L: 300000, 2L: 150000 }
     */
    @NotNull(message = "카테고리별 지출액은 필수입니다.")
    private Map<Long, Long> categoryAmounts;
}
