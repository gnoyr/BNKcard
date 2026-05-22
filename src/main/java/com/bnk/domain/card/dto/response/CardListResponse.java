package com.bnk.domain.card.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CardListResponse {
    private Long cardId;
    private String cardName;
    private String companyName;
    private String cardType;
    private String cardStatus;
    private LocalDateTime publishStartAt;  // 추가
    private LocalDateTime publishEndAt;
    private String thumbnailUrl;        // CARD_IMAGES(THUMBNAIL)
    private String topBenefit;          // CARD_BENEFITS.display_text (display_order=1)
    private String recommendReason;     // TOP3 추천 시 사용
}
