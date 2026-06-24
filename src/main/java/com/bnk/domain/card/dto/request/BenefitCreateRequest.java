package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 혜택 개별 등록 요청 DTO
 *
 * 변경 이력:
 *  - Bean Validation 추가 (model.CardBenefit validation과 일치)
 *  - dto.request2.BenefitCreateRequest 삭제 대상 (validation 없던 초기 버전)
 */
@Getter
@NoArgsConstructor
public class BenefitCreateRequest {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @NotBlank(message = "혜택 제목은 필수입니다.")
    @Size(max = 200, message = "혜택 제목은 200자 이하여야 합니다.")
    private String benefitTitle;

    @NotBlank(message = "혜택 타입은 필수입니다.")
    @Pattern(regexp = "RATE_DISCOUNT|FIXED_DISCOUNT|POINT|CASHBACK|FREE",
             message = "혜택 타입은 RATE_DISCOUNT, FIXED_DISCOUNT, POINT, CASHBACK, FREE 중 하나여야 합니다.")
    private String benefitType;

    @DecimalMin(value = "0.0000", message = "할인율은 0 이상이어야 합니다.")
    @DecimalMax(value = "1.0000", message = "할인율은 1.0 이하여야 합니다.")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal discountRate;

    @Min(value = 0, message = "할인액은 0 이상이어야 합니다.")
    private Long discountAmount;

    @DecimalMin(value = "0.0000") @DecimalMax(value = "1.0000")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal pointRate;

    @DecimalMin(value = "0.0000") @DecimalMax(value = "1.0000")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal cashbackRate;

    @Min(value = 0, message = "월 혜택 한도는 0 이상이어야 합니다.")
    private Long monthlyLimitAmount;

    @Min(value = 0)
    private Long dailyLimitAmount;

    @Min(value = 0)
    private Long minimumPaymentAmount;

    private String benefitCondition;   // CLOB — 길이 제한 없음

    @Size(max = 300, message = "UI 표시용 요약은 300자 이하여야 합니다.")
    private String displayText;

    @Min(value = 1)
    private Integer displayOrder;

    @Pattern(regexp = "Y|N", message = "노출 여부는 Y 또는 N이어야 합니다.")
    private String visibleYn = "Y";
}
