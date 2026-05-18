package com.bnk.domain.spending.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SpendingPatternRequest {

    @NotEmpty(message = "소비패턴 목록은 1개 이상이어야 합니다.")
    @Valid
    private List<PatternItem> patterns;

    @Getter
    @NoArgsConstructor
    public static class PatternItem {

        @NotNull(message = "카테고리 ID는 필수입니다.")
        private Long categoryId;

        @NotNull(message = "월 소비금액은 필수입니다.")
        @Min(value = 0, message = "월 소비금액은 0 이상이어야 합니다.")
        private Long monthlyAmount;
    }
}
