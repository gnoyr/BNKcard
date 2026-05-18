package com.bnk.domain.search.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KeywordMappingRequest {

    @NotEmpty(message = "키워드 ID 목록은 1개 이상이어야 합니다.")
    @Size(max = 50, message = "한 번에 최대 50개까지 매핑할 수 있습니다.")
    private List<Long> keywordIds;
}
