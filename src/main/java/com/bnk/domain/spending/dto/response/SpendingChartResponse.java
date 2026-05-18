package com.bnk.domain.spending.dto.response;

import com.bnk.domain.spending.model.SpendingPattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpendingChartResponse {
    private Long categoryId;
    private String categoryName;
    private String iconCode;
    private Long monthlyAmount;
    private double ratio;               // 전체 합 대비 비중 (%)

    public static SpendingChartResponse of(SpendingPattern p, long total) {
        double ratio = total == 0 ? 0 : Math.round((double) p.getMonthlyAmount() / total * 1000) / 10.0;
        return SpendingChartResponse.builder()
                .categoryId(p.getCategoryId())
                .categoryName(p.getCategoryName())
                .iconCode(p.getIconCode())
                .monthlyAmount(p.getMonthlyAmount())
                .ratio(ratio)
                .build();
    }
}
