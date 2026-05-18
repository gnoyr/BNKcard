package com.bnk.domain.card.model2;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardBenefit {

	private Long benefitId;                 // NUMBER(19) PK, 자동 채번

    @NotNull(message = "카드 ID는 필수입니다.")
    private Long cardId;                    // NUMBER(19) FK NN

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;               // NUMBER(19) FK NN — 소비패턴 추천 JOIN 기준

    @NotBlank(message = "혜택 제목은 필수입니다.")
    @Size(max = 200, message = "혜택 제목은 200자 이하여야 합니다.")
    private String benefitTitle;            // VARCHAR2(200) NN

    @NotBlank(message = "혜택 타입은 필수입니다.")
    @Pattern(regexp = "RATE_DISCOUNT|FIXED_DISCOUNT|POINT|CASHBACK|FREE",
             message = "혜택 타입은 RATE_DISCOUNT, FIXED_DISCOUNT, POINT, CASHBACK, FREE 중 하나여야 합니다.")
    private String benefitType;             // VARCHAR2(30) NN

    // NUMBER(5,4) → BigDecimal (Oracle 소수 정밀도 보장, 예: 10% → 0.1000)
    @DecimalMin(value = "0.0000", message = "할인율은 0 이상이어야 합니다.")
    @DecimalMax(value = "1.0000", message = "할인율은 1.0 이하여야 합니다.")
    @Digits(integer = 1, fraction = 4, message = "할인율은 소수점 4자리까지 입력 가능합니다.")
    private BigDecimal discountRate;        // NUMBER(5,4) NULL

    @Min(value = 0, message = "할인액은 0 이상이어야 합니다.")
    private Long discountAmount;           // NUMBER(12) NULL

    @DecimalMin(value = "0.0000", message = "적립율은 0 이상이어야 합니다.")
    @DecimalMax(value = "1.0000", message = "적립율은 1.0 이하여야 합니다.")
    @Digits(integer = 1, fraction = 4, message = "적립율은 소수점 4자리까지 입력 가능합니다.")
    private BigDecimal pointRate;           // NUMBER(5,4) NULL

    @DecimalMin(value = "0.0000", message = "캐시백율은 0 이상이어야 합니다.")
    @DecimalMax(value = "1.0000", message = "캐시백율은 1.0 이하여야 합니다.")
    @Digits(integer = 1, fraction = 4, message = "캐시백율은 소수점 4자리까지 입력 가능합니다.")
    private BigDecimal cashbackRate;        // NUMBER(5,4) NULL

    @Min(value = 0, message = "월 혜택 한도는 0 이상이어야 합니다.")
    private Long monthlyLimitAmount;       // NUMBER(12) NULL — NULL=무제한

    @Min(value = 0, message = "일 혜택 한도는 0 이상이어야 합니다.")
    private Long dailyLimitAmount;         // NUMBER(12) NULL

    @Min(value = 0, message = "최소 결제금액은 0 이상이어야 합니다.")
    private Long minimumPaymentAmount;     // NUMBER(12) NULL

    private String benefitCondition;       // CLOB NULL

    @Size(max = 300, message = "UI 표시용 요약은 300자 이하여야 합니다.")
    private String displayText;            // VARCHAR2(300) NULL

    private Integer displayOrder;          // NUMBER(5) NULL

    @NotBlank(message = "노출 여부는 필수입니다.")
    @Pattern(regexp = "Y|N", message = "노출 여부는 Y 또는 N이어야 합니다.")
    private String visibleYn = "Y";        // CHAR(1) DEFAULT 'Y' NN

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
