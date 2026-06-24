package com.bnk.domain.card.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class BannerDto {
    private Long cardId;
    private String cardName;
    private String bannerImageUrl;      // CARD_IMAGES(FRONT) 또는 CARD_PROMOTIONS.banner_image_url
    private String companyName;
}
