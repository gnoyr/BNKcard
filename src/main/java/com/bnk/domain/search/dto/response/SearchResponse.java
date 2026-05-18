package com.bnk.domain.search.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchResponse {
    private Long cardId;
    private String cardName;
    private String thumbnailUrl;
    private String topBenefit;
    private String companyName;
}
