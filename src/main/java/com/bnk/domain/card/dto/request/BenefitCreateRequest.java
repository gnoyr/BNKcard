package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter @NoArgsConstructor
public class BenefitCreateRequest {

    @NotBlank(message = "혜택 제목은 필수입니다.")
    @Size(max = 200)
    private String benefitTitle;

    @NotBlank(message = "혜택 유형은 필수입니다.")
    @Pattern(regexp = "^(RATE_DISCOUNT|FIXED_DISCOUNT|POINT|CASHBACK|FREE)$")
    private String benefitType;

    private Long categoryId;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private BigDecimal discountRate;

    @Min(0)
    private Long discountAmount;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private BigDecimal pointRate;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private BigDecimal cashbackRate;

    @Min(0)
    private Long monthlyLimitAmount;

    @Min(0)
    private Long dailyLimitAmount;

    @Min(0)
    private Long minimumPaymentAmount;

    @Size(max = 300)
    private String displayText;

    private Integer displayOrder;
}
