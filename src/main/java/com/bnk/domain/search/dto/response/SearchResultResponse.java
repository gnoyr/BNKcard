package com.bnk.domain.search.dto.response;

import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.global.response.PageResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchResultResponse {
    private PageResponse<CardListResponse> page;
    private boolean aiSearched;          // AI 검색 사용 여부
    private String correctedQuery;       // 교정된 검색어 (AI가 유추한 것)
    private String aiSearchMessage;      // "약관으로 검색한 결과입니다"
}