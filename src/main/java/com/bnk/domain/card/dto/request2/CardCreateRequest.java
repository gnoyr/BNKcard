package com.bnk.domain.card.dto.request2;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardCreateRequest {
	
	// ── CARDS ────────────────────────────────────────────
    private String        cardCode;
    private String        cardType;             // CREDIT / CHECK / HYBRID / PREPAID
    private String        cardName;
    private String        companyName;
    private String        brandName;
    private Integer       annualFeeDomestic;
    private Integer       annualFeeOverseas;
    private Long          previousMonthSpend;
    private Integer       minimumAge;
    private Integer       maximumAge;
    private Long          creditLimitMin;
    private Long          creditLimitMax;
    private String        targetUser;
    private String        summaryDescription;
    private String        searchableYn;
    private String        visibleYn;
    private String        approvalRequiredYn;
    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;

    // ── CARD_BENEFITS ────────────────────────────────────
    private List<BenefitCreateRequest> benefits;

    // ── CARD_IMAGES ──────────────────────────────────────
    private List<ImageCreateRequest> images;
    
    // ── APPROVAL_REQUESTS ───────────────────────────
    private String requestTypeCode;  // CARD_PUBLISH / CARD_UPDATE
    private String requestComment;

}
