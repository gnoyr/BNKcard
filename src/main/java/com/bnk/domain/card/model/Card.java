package com.bnk.domain.card.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CARDS 테이블 단일 모델 (구 model.Card + model2.Card 통합)
 *
 * 변경 이력:
 *  - model2.Card Bean Validation 어노테이션 흡수
 *  - @JsonAlias("updateBy"), @JsonAlias("updateAt") 추가
 *    → 기존 스냅샷 JSON(model2.Card 직렬화본)의 updateBy/updateAt 키를
 *      updatedBy/updatedAt 필드로 역직렬화할 수 있도록 호환성 보장
 *  - @Setter 유지 (CardMapper XML keyProperty 주입용)
 *  - model2.Card 삭제 대상: com.bnk.domain.card.model2.Card
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    private Long cardId;

    @NotBlank(message = "카드 코드는 필수입니다.")
    @Size(max = 50, message = "카드 코드는 50자 이하여야 합니다.")
    private String cardCode;

    @NotBlank(message = "카드 타입은 필수입니다.")
    @Pattern(regexp = "CREDIT|CHECK|PREPAID", message = "카드 타입은 CREDIT, CHECK, PREPAID 중 하나여야 합니다.")
    private String cardType;

    @NotBlank(message = "카드명은 필수입니다.")
    @Size(max = 200, message = "카드명은 200자 이하여야 합니다.")
    private String cardName;

    @NotBlank(message = "카드사는 필수입니다.")
    @Size(max = 100, message = "카드사명은 100자 이하여야 합니다.")
    private String companyName;

    private String companyCode;   // DEFAULT '01', FN_GEN_CARD_ID 계산 기준

    @Pattern(regexp = "VISA|MASTER|LOCAL|AMEX|UNIONPAY",
             message = "네트워크는 VISA, MASTER, LOCAL, AMEX, UNIONPAY 중 하나여야 합니다.")
    private String brandName;

    @NotNull(message = "국내 연회비는 필수입니다.")
    @Min(value = 0, message = "국내 연회비는 0 이상이어야 합니다.")
    @Builder.Default
    private Long annualFeeDomestic = 0L;

    @Min(value = 0, message = "해외 연회비는 0 이상이어야 합니다.")
    @Builder.Default
    private Long annualFeeOverseas = 0L;

    @Min(value = 0, message = "전월실적은 0 이상이어야 합니다.")
    @Builder.Default
    private Long previousMonthSpend = 0L;

    @Min(value = 0, message = "최소 나이는 0 이상이어야 합니다.")
    @Max(value = 150)
    private Integer minimumAge;

    @Min(value = 0, message = "최대 나이는 0 이상이어야 합니다.")
    @Max(value = 150)
    private Integer maximumAge;

    @Min(value = 0, message = "최소 한도는 0 이상이어야 합니다.")
    private Long creditLimitMin;

    @Min(value = 0, message = "최대 한도는 0 이상이어야 합니다.")
    private Long creditLimitMax;

    @Size(max = 300, message = "추천 대상은 300자 이하여야 합니다.")
    private String targetUser;

    @Size(max = 1000, message = "카드 요약 설명은 1000자 이하여야 합니다.")
    private String summaryDescription;

    @Pattern(regexp = "Y|N", message = "검색 노출 여부는 Y 또는 N이어야 합니다.")
    @Builder.Default
    private String searchableYn = "Y";

    @Pattern(regexp = "Y|N", message = "화면 노출 여부는 Y 또는 N이어야 합니다.")
    @Builder.Default
    private String visibleYn = "Y";

    @Pattern(regexp = "Y|N", message = "결재 필요 여부는 Y 또는 N이어야 합니다.")
    @Builder.Default
    private String approvalRequiredYn = "Y";

    @Pattern(regexp = "DRAFT|REVIEW|APPROVED|PUBLISHED|STOPPED|EXPIRED",
             message = "카드 상태는 DRAFT, REVIEW, APPROVED, PUBLISHED, STOPPED, EXPIRED 중 하나여야 합니다.")
    @Builder.Default
    private String cardStatus = "DRAFT";

    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;

    @Builder.Default
    private Long applicationCount = 0L;

    /** 사용자 카드 목록 조회 시 JOIN 없이 직접 조회되는 viewCount 필드 (model2에 없던 필드) */
    private Long viewCount;

    private Long createdBy;
    private LocalDateTime createdAt;

    /**
     * @JsonAlias: 구 model2.Card 스냅샷 JSON 역직렬화 호환
     * model2.Card는 필드명이 updateBy(오타)였으므로, 기존 스냅샷 JSON 키가 "updateBy"
     * → 이 어노테이션이 없으면 결재 승인 시 역직렬화 후 updatedBy = null이 됨
     */
    @JsonAlias("updateBy")
    private Long updatedBy;

    @JsonAlias("updateAt")
    private LocalDateTime updatedAt;

    @Pattern(regexp = "Y|N")
    @Builder.Default
    private String deletedYn = "N";

    private LocalDateTime deletedAt;
}
