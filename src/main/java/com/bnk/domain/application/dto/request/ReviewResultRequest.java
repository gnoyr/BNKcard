package com.bnk.domain.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReviewResultRequest {
    private Long    appId;
    private String  applicationStatus;  // APPROVED / REJECTED / PENDING_LIMIT
    private Long    approvedLimit;
    private String  rejectionReason;
    private String  reviewedBy;

    // PENDING_LIMIT 케이스 — BNKcard가 재심사에 사용할 신용정보
    private Long    estimatedMonthlyIncome;
    private Long   monthlyPayment;  		// 월 납부액 (기존 대출 상환액)
    private Integer creditScore;
    private Integer vehicleCount;
    private Long    loanBalance;
    private Double  delinquencyRate;
    private Integer multipleDebtCount;
    private String  jobType;
}
