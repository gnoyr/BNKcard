package com.bnk.domain.card.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CARD_BENEFITS 테이블 단일 모델 (구 model.CardBenefit + model2.CardBenefit 통합)
 *
 * 변경 이력:
 *  - model2.CardBenefit Bean Validation 어노테이션 흡수
 *  - JOIN 필드(categoryName, iconCode)는 model.CardBenefit 기준 유지
 *    → CardBenefitMapper.findByCardId() 결과에서 CARD_CATEGORIES JOIN으로 채워짐
 *  - model2.CardBenefit 삭제 대상: com.bnk.domain.card.model2.CardBenefit
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBenefit {

    private Long benefitId;

    @NotNull(message = "카드 ID는 필수입니다.")
    private Long cardId;

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
    @Digits(integer = 1, fraction = 4, message = "할인율은 소수점 4자리까지 입력 가능합니다.")
    private BigDecimal discountRate;

    @Min(value = 0, message = "할인액은 0 이상이어야 합니다.")
    private Long discountAmount;

    @DecimalMin(value = "0.0000", message = "적립율은 0 이상이어야 합니다.")
    @DecimalMax(value = "1.0000", message = "적립율은 1.0 이하여야 합니다.")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal pointRate;

    @DecimalMin(value = "0.0000", message = "캐시백율은 0 이상이어야 합니다.")
    @DecimalMax(value = "1.0000", message = "캐시백율은 1.0 이하여야 합니다.")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal cashbackRate;

    @Min(value = 0, message = "월 혜택 한도는 0 이상이어야 합니다.")
    private Long monthlyLimitAmount;

    @Min(value = 0, message = "일 혜택 한도는 0 이상이어야 합니다.")
    private Long dailyLimitAmount;

    @Min(value = 0, message = "최소 결제금액은 0 이상이어야 합니다.")
    private Long minimumPaymentAmount;

    private String benefitCondition;   // CLOB

    @Size(max = 300, message = "UI 표시용 요약은 300자 이하여야 합니다.")
    private String displayText;

    private Integer displayOrder;

    @Pattern(regexp = "Y|N", message = "노출 여부는 Y 또는 N이어야 합니다.")
    @Builder.Default
    private String visibleYn = "Y";

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── JOIN 필드 (CARD_CATEGORIES JOIN 결과) ───────────────────────
    /** model.CardBenefit 기존 필드 — 소비패턴 추천 시 categoryName 직접 노출 */
    private String categoryName;
    private String iconCode;
}
