package com.bnk.domain.card.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter @NoArgsConstructor
public class CardCreateRequest {

    @NotBlank(message = "카드 코드는 필수입니다.")
    @Size(max = 50)
    private String cardCode;

    @NotBlank(message = "카드 유형은 필수입니다.")
    @Pattern(regexp = "^(CREDIT|CHECK|HYBRID|PREPAID)$", message = "카드 유형은 CREDIT, CHECK, HYBRID 중 하나여야 합니다.")
    private String cardType;

    @NotBlank(message = "카드명은 필수입니다.")
    @Size(max = 200)
    private String cardName;

    @NotBlank(message = "카드사명은 필수입니다.")
    @Size(max = 100)
    private String companyName;

    @Size(max = 50)
    private String brandName;

    @NotNull(message = "국내 연회비는 필수입니다.")
    @Min(value = 0)
    private Long annualFeeDomestic;

    @Min(value = 0)
    private Long annualFeeOverseas;

    @Size(max = 1000)
    private String summaryDescription;

    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;
    
    @NotNull(message = "전월 실적은 필수입니다.")
    @Min(value = 0)
    private Long previousMonthSpend;

    // nullable, 선택 입력
    @Min(value = 0) private Integer minimumAge;
    @Min(value = 0) private Integer maximumAge;

    @Size(max = 300) private String targetUser;

    @Valid
    private List<BenefitCreateRequest> benefits;

    @Valid
    private List<ImageCreateRequest> images;

    @NotBlank(message = "변경 사유는 필수입니다.")
    @Size(max = 2000)
    private String changeSummary;       // 수정 시 CARD_VERSIONS.change_summary
}
