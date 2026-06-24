package com.bnk.domain.card.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 카드 신규 등록 요청 DTO
 *
 * 변경 이력:
 *  - cardType 패턴: CREDIT|CHECK|HYBRID|PREPAID → CREDIT|CHECK|PREPAID
 *    (model.Card, DB 트리거 FN_GEN_CARD_ID 기준과 통일)
 *  - @Setter: MyBatis keyProperty 주입 아님 — @ModelAttribute 바인딩용으로 유지
 *  - dto.request2.CardCreateRequest 삭제 대상 (이 클래스로 대체)
 */
@Setter
@Getter
@NoArgsConstructor
public class CardCreateRequest {

    @NotBlank(message = "카드 코드는 필수입니다.")
    @Size(max = 50, message = "카드 코드는 50자 이하여야 합니다.")
    private String cardCode;

    @NotBlank(message = "카드 유형은 필수입니다.")
    @Pattern(regexp = "CREDIT|CHECK|PREPAID",
             message = "카드 유형은 CREDIT, CHECK, PREPAID 중 하나여야 합니다.")
    private String cardType;

    @NotBlank(message = "카드명은 필수입니다.")
    @Size(max = 200, message = "카드명은 200자 이하여야 합니다.")
    private String cardName;

    @NotBlank(message = "카드사명은 필수입니다.")
    @Size(max = 100, message = "카드사명은 100자 이하여야 합니다.")
    private String companyName;

    @Size(max = 10, message = "카드사 코드는 10자 이하여야 합니다.")
    private String companyCode;

    @Pattern(regexp = "VISA|MASTER|LOCAL|AMEX|UNIONPAY",
             message = "네트워크는 VISA, MASTER, LOCAL, AMEX, UNIONPAY 중 하나여야 합니다.")
    @Size(max = 50)
    private String brandName;

    @NotNull(message = "국내 연회비는 필수입니다.")
    @Min(value = 0, message = "국내 연회비는 0 이상이어야 합니다.")
    private Long annualFeeDomestic;

    @Min(value = 0, message = "해외 연회비는 0 이상이어야 합니다.")
    private Long annualFeeOverseas;

    @NotNull(message = "전월 실적은 필수입니다.")
    @Min(value = 0, message = "전월 실적은 0 이상이어야 합니다.")
    private Long previousMonthSpend;

    @Min(value = 0) @Max(value = 150)
    private Integer minimumAge;

    @Min(value = 0) @Max(value = 150)
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
    private String searchableYn;

    @Pattern(regexp = "Y|N", message = "화면 노출 여부는 Y 또는 N이어야 합니다.")
    private String visibleYn;

    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;

    @Valid
    private List<BenefitCreateRequest> benefits;

    @Valid
    private List<ImageCreateRequest> images;

    @NotBlank(message = "변경 사유는 필수입니다.")
    @Size(max = 2000, message = "변경 사유는 2000자 이하여야 합니다.")
    private String changeSummary;
}
