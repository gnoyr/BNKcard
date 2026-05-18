package com.bnk.domain.card.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private Long cardId;
    private String cardCode;
    private String cardType;            // CREDIT / CHECK / HYBRID
    private String cardName;
    private String companyName;
    private String brandName;
    private Long annualFeeDomestic;
    private Long annualFeeOverseas;
    private Long previousMonthSpend;
    private Integer minimumAge;
    private Integer maximumAge;
    private String targetUser;
    private String summaryDescription;
    private String searchableYn;
    private String visibleYn;
    private String approvalRequiredYn;
    private String cardStatus;          // DRAFT/REVIEW/APPROVED/PUBLISHED/STOPPED/EXPIRED
    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;
    private Long applicationCount;
    private Long viewCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private String deletedYn;
}
