package com.bnk.domain.search.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopularKeywordResponse {
    private String keyword;
    private Long searchCount;
}
