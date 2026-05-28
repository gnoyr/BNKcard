package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 관리자 카드 목록 다중조건 검색 요청 DTO
 * GET /api/admin/cards — @ModelAttribute 바인딩
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminCardSearchRequest {

    /** 통합 키워드 (카드명, 카드사, 검색키워드 대상) */
    private String keyword;

    private String cardName;
    private String companyName;
    private String cardType;      // CREDIT / CHECK / PREPAID
    private String cardStatus;    // DRAFT / REVIEW / APPROVED / PUBLISHED / STOPPED / EXPIRED
    private String brandName;     // VISA / MASTER / LOCAL / AMEX / UNIONPAY

    private LocalDateTime publishStartFrom;
    private LocalDateTime publishStartTo;

    /** 정렬 기준: cardName / annualFee / applicationCount / 기본=created_at DESC */
    private String sort;

    @Min(value = 0)
    private int page = 0;

    @Min(value = 1)
    private int size = 20;

    public int getOffset() {
        return page * size;
    }
}
