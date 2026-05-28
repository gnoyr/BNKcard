package com.bnk.domain.card.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카드 기본정보 수정 요청 DTO
 *
 * 모든 필드가 선택(null 허용) — null이면 기존 값을 유지.
 * CARD_VERSIONS snapshot + APPROVAL_REQUESTS 생성 흐름에서 사용.
 */
@Setter
@Getter
@NoArgsConstructor
public class CardUpdateRequest {

    @Pattern(regexp = "CREDIT|CHECK|PREPAID",
             message = "카드 유형은 CREDIT, CHECK, PREPAID 중 하나여야 합니다.")
    private String cardType;

    @Size(max = 200, message = "카드명은 200자 이하여야 합니다.")
    private String cardName;

    @Size(max = 100, message = "카드사명은 100자 이하여야 합니다.")
    private String companyName;

    @Pattern(regexp = "VISA|MASTER|LOCAL|AMEX|UNIONPAY")
    @Size(max = 50)
    private String brandName;

    @Min(value = 0, message = "국내 연회비는 0 이상이어야 합니다.")
    private Long annualFeeDomestic;

    @Min(value = 0)
    private Long annualFeeOverseas;

    @Min(value = 0)
    private Long previousMonthSpend;

    @Min(value = 0) @Max(value = 150)
    private Integer minimumAge;

    @Min(value = 0) @Max(value = 150)
    private Integer maximumAge;

    @Min(value = 0)
    private Long creditLimitMin;

    @Min(value = 0)
    private Long creditLimitMax;

    @Size(max = 300)
    private String targetUser;

    @Size(max = 1000)
    private String summaryDescription;

    @Pattern(regexp = "Y|N")
    private String searchableYn;

    @Pattern(regexp = "Y|N")
    private String visibleYn;

    /** 논리 삭제 처리용 (Y 설정 시 deleted_at도 서비스에서 자동 세팅) */
    @Pattern(regexp = "Y|N")
    private String deletedYn;

    /** 상태 변경이 필요할 때만 설정 (null이면 기존 상태 유지) */
    @Pattern(regexp = "DRAFT|REVIEW|APPROVED|PUBLISHED|STOPPED|EXPIRED")
    private String cardStatus;

    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;

    @NotBlank(message = "변경 사유는 필수입니다.")
    @Size(max = 2000)
    private String changeSummary;
}
