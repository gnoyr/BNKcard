package com.bnk.domain.card.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardCompareResponse {
	private Long cardId;
	private String cardName;
	private String companyName;
	private String cardType;
	private Long annualFeeDomestic;
	private Long annualFeeOverseas;
	private String thumbnailUrl;
	private List<BenefitDto> benefits;
}