package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 사용자 카드 목록/검색 요청 DTO
 * GET /api/cards — @ModelAttribute 바인딩
 *
 * 변경 이력:
 *  - sort 필드 추가 (XML <choose> 조건과 일치)
 *  - dto.request2.CardSearchRequest 삭제 대상 (keyword만 있던 단순 버전)
 */
@Getter
@Setter
@NoArgsConstructor
public class CardSearchRequest {

    /** 통합 검색어 (카드명, 설명, 카드사, 키워드 대상) */
    private String q;

    private String cardType;     // CREDIT / CHECK / PREPAID
    private String companyName;

    /** 단일 카테고리 필터 */
    private Long categoryId;

    /** 다중 카테고리 교집합 필터 */
    private List<Long> categoryIds;

    @Min(value = 0)
    private Long minFee;

    @Min(value = 0)
    private Long maxFee;

    /** 정렬 기준: cardName / annualFee / applicationCount / 기본=view_count DESC */
    private String sort;

    @Min(value = 0)
    private int page = 0;

    @Min(value = 1)
    private int size = 20;

    public int getOffset() {
        return page * size;
    }

    /** categoryIds 다중선택 교집합 조건 — MyBatis XML에서 사용 */
    public int getCategoryCount() {
        return (categoryIds != null) ? categoryIds.size() : 0;
    }

    /**
     * categoryId를 String으로 받는 setter.
     * @ModelAttribute 바인딩 시 쿼리파라미터 "categoryId=123"을 파싱.
     */
    public void setCategoryId(String categoryId) {
        if (categoryId != null && !categoryId.isBlank()) {
            try {
                this.categoryId = Long.parseLong(categoryId.trim());
            } catch (NumberFormatException ignored) {
                this.categoryId = null;
            }
        }
    }
}
