package com.bnk.domain.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보유 카드(USER_CARDS) 부분 수정 요청.
 *
 * 모든 필드는 nullable — 전달된(=null 이 아닌) 필드만 부분 업데이트한다.
 * 보안상 사용자가 직접 변경할 수 있는 컬럼만 허용한다.
 * (카드번호·연결계좌·발급정보 등은 변경 불가)
 */
@Getter
@Setter
@NoArgsConstructor
public class UserCardUpdateRequest {

    /** 일일 한도 (CARD_VERSIONS 최대한도 이하) */
    @Min(value = 0, message = "일일 한도는 0 이상이어야 합니다.")
    private Long dailyLimitAmount;

    /** 월 한도 */
    @Min(value = 0, message = "월 한도는 0 이상이어야 합니다.")
    private Long monthlyLimitAmount;

    /** 해외 사용 여부 Y/N */
    @Pattern(regexp = "Y|N", message = "해외 사용 여부는 Y 또는 N이어야 합니다.")
    private String overseasEnabledYn;

    /** 비접촉 결제 여부 Y/N */
    @Pattern(regexp = "Y|N", message = "비접촉 결제 여부는 Y 또는 N이어야 합니다.")
    private String contactlessEnabledYn;

    /** 카드 별칭 */
    @Size(max = 50, message = "카드 별칭은 50자 이하여야 합니다.")
    private String cardNickname;

    /** 월 결제일 (1~31) */
    @Min(value = 1, message = "결제일은 1 이상이어야 합니다.")
    @Max(value = 31, message = "결제일은 31 이하여야 합니다.")
    private Integer paymentDay;

    /** 거래 알림 방식 */
    @Pattern(regexp = "SMS|PUSH|NONE", message = "거래 알림 방식은 SMS, PUSH, NONE 중 하나여야 합니다.")
    private String txAlertType;

    /** 청구서 방식 */
    @Pattern(regexp = "EMAIL|APP|PAPER", message = "청구서 방식은 EMAIL, APP, PAPER 중 하나여야 합니다.")
    private String statementMethod;

    /** 카드 상태 — 사용자가 직접 변경 가능한 값만 허용 (정상/일시정지/분실) */
    @Pattern(regexp = "ACTIVE|STOPPED|LOST",
             message = "카드 상태는 ACTIVE, STOPPED, LOST 중 하나여야 합니다.")
    private String cardStatus;
}
