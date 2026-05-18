package com.bnk.domain.card.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CardCompareResponse {
    private Long cardId;
    private String cardName;
    private String companyName;
    private Long annualFeeDomestic;
    private List<BenefitDto> benefits;
}
