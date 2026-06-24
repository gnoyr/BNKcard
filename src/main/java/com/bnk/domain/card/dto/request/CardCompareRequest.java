// ──────────────────────────────────────────────────────────────────────────────
// CardCompareRequest.java
// ──────────────────────────────────────────────────────────────────────────────
package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드 비교 요청 DTO — POST /api/cards/compare
 * 최소 2개, 최대 3개 카드 선택.
 */
@Getter
@NoArgsConstructor
public class CardCompareRequest {

    @NotNull(message = "비교할 카드 ID 목록은 필수입니다.")
    @Size(min = 2, max = 3, message = "카드는 2~3개를 선택해주세요.")
    private List<Long> cardIds;

    /** 시뮬레이션 포함 시 월 지출액 (선택) */
    @Min(value = 0)
    private Long monthlySpend;
}
