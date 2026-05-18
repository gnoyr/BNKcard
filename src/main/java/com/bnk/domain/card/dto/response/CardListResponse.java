package com.bnk.domain.card.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CardListResponse {
    private Long cardId;
    private String cardName;
    private String companyName;
    private String cardType;
    private Long annualFeeDomestic;
    private String thumbnailUrl;        // CARD_IMAGES(THUMBNAIL)
    private String topBenefit;          // CARD_BENEFITS.display_text (display_order=1)
    private String recommendReason;     // TOP3 추천 시 사용
}
