package com.bnk.domain.card.model2;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Card {
	
	private Long cardId;
	
	@NotBlank(message = "카드 코드는 필수입니다.")
    @Size(max = 50, message = "카드 코드는 50자 이하여야 합니다.")
	private String cardCode;
	
	@NotBlank(message = "카드 타입은 필수입니다.")
	@Pattern(regexp = "CREDIT|CHECK|PREPAID", message = "카드 타입은 CREDIT, CHECK, PREPAID 중 하여여야 합니다.")
	private String cardType;
	
	@NotBlank(message = "카드명은 필수입니다.")
	@Size(max = 200, message = "카드명은 200자 이하여야 합니다.")
	private String cardName;
	
	@NotBlank(message = "카드사는 필수입니다.")
	@Size(max =100, message = "카드사명은 100자 이하여야 합니다.")
	private String companyName;	
	
	// Card model2 클래스 내 기존 필드들 사이에 추가
	private String companyCode;   // DEFAULT '01', FN_GEN_CARD_ID 계산 기준
	
	@Pattern(regexp = "VISA|MASTER|LOCAL|AMEX|UNIONPAY", message = "네트워크는 VISA, MASTERCARD, LOCAL, AMEX, UNIONPAY 중 하나여야 합니다.")
	private String brandName;
	
	@NotNull(message = "국내 연회비는 필수입니다.")
    @Min(value = 0, message = "국내 연회비는 0 이상이어야 합니다.")
	private Integer annualFeeDomestic = 0;  // NUMBER(10) DEFAULT 0 NN
	
	@NotNull(message = "해외 연회비는 필수입니다.")
    @Min(value = 0, message = "해외 연회비는 0 이상이어야 합니다.")
	private Integer annualFeeOverseas = 0;  // NUMBER(10) DEFAULT 0 NN	
	
	@NotNull(message = "전월실적은 필수입니다.")
    @Min(value = 0, message = "전월실적은 0 이상이어야 합니다.")
	private Long previousMonthSpend = 0L;   // NUMBER(12) DEFAULT 0 NN
	
	@Min(value = 0, message = "최소 나이는 0 이상이어야 합니다.")
    @Max(value = 150, message = "최소 나이는 150 이하여야 합니다.")
	private Integer minimumAge;
	
	@Min(value = 0, message = "최대 나이는 0 이상이어야 합니다.")
    @Max(value = 150, message = "최대 나이는 150 이하여야 합니다.")
	private Integer maximumAge;
	
	@Min(value = 0, message = "최소 한도는 0 이상이어야 합니다.")
	private Long creditLimitMin;  // 체크카드 null 허용
	
	@Min(value = 0, message = "최대 한도는 0 이상이어야 합니다.")
	private Long creditLimitMax;  // 체크카드 null 허용
	
	@Size(max = 300, message = "추천 대상은 300자 이하여야 합니다.")
	private String targetUser;
	
	@Size(max = 1000, message = "카드 요약 설명은 1000자 이하여야 합니다.")
	private String summaryDescription;
	
	@NotBlank(message = "검색 노출 여부는 필수입니다.")
    @Pattern(regexp = "Y|N", message = "검색 노출 여부는 Y 또는 N이어야 합니다.")
	private String searchableYn = "Y";    // CHAR(1) DEFAULT 'Y' NN
	
	@NotBlank(message = "화면 노출 여부는 필수입니다.")
    @Pattern(regexp = "Y|N", message = "화면 노출 여부는 Y 또는 N이어야 합니다.")
    private String visibleYn = "Y";       // CHAR(1) DEFAULT 'Y' NN
	
	@NotBlank(message = "결재 필요 여부는 필수입니다.")
    @Pattern(regexp = "Y|N", message = "결재 필요 여부는 Y 또는 N이어야 합니다.")
    private String approvalRequiredYn = "Y"; // CHAR(1) DEFAULT 'Y' NN
	
	@NotBlank(message = "카드 상태는 필수입니다.")
    @Pattern(regexp = "DRAFT|REVIEW|APPROVED|PUBLISHED|STOPPED|EXPIRED",
             message = "카드 상태는 DRAFT, REVIEW, APPROVED, PUBLISHED, STOPPED, EXPIRED 중 하나여야 합니다.")
    private String cardStatus = "DRAFT";    // VARCHAR2(30) DEFAULT 'DRAFT' NN
	
	private LocalDateTime publishStartAt;
	private LocalDateTime publishEndAt;
	
	private Integer applicationCount = 0;
	
	private Long createdBy;      
	private LocalDateTime createdAt;	
	
	private Long updateBy;
	private LocalDateTime updateAt;
	
	@Pattern(regexp = "Y|N", message = "삭제 여부는 Y 또는 N이어야 합니다.")
    private String deletedYn = "N";         // CHAR(1) DEFAULT 'N' NN
	private LocalDateTime deleteAt;
}
