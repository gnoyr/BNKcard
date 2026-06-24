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
    private Long annualFeeDomestic;    
    private Long annualFeeOverseas;    
    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;
    private String thumbnailUrl;
    private String topBenefit;
    private String recommendReason;
}
