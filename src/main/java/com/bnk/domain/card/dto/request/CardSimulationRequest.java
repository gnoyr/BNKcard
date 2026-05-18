package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter @NoArgsConstructor
public class CardSimulationRequest {

    @NotNull
    @Size(min = 1, max = 3, message = "카드는 1~3개를 선택해주세요.")
    private List<Long> cardIds;

    /** categoryId → monthlyAmount. MIN(amount * rate, limit) 공식 */
    @NotEmpty(message = "카테고리별 월 지출액은 필수입니다.")
    private Map<Long, Long> categoryAmounts;
}
