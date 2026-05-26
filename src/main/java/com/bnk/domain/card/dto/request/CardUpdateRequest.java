package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CardUpdateRequest {

    @NotBlank(message = "수정 사유는 필수입니다.")
    @Size(max = 2000)
    private String changeSummary;

    // 수정 가능 필드 — 모두 nullable (null이면 기존값 유지)
    @Size(max = 200)
    private String cardName;

    @Min(0)
    private Long annualFeeDomestic;

    @Min(0)
    private Long annualFeeOverseas;

    @Min(0)
    private Long previousMonthSpend;

    @Min(0)
    private Integer minimumAge;

    @Min(0)
    private Integer maximumAge;

    @Min(0)
    private Long creditLimitMin;

    @Min(0)
    private Long creditLimitMax;

    @Size(max = 300)
    private String targetUser;

    @Size(max = 1000)
    private String summaryDescription;

    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;

    @Pattern(regexp = "Y|N")
    private String searchableYn;

    @Pattern(regexp = "Y|N")
    private String visibleYn;
}