package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @NoArgsConstructor
public class CardCompareRequest {

    @NotNull
    @Size(min = 2, max = 3, message = "카드는 2~3개를 선택해주세요.")
    private List<Long> cardIds;

    private Long monthlySpend;
}
