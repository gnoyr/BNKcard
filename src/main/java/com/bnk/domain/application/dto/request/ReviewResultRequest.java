package com.bnk.domain.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReviewResultRequest {
    private Long   appId;
    private String applicationStatus; // APPROVED / REJECTED / PENDING_LIMIT
    private Long   approvedLimit;
    private String rejectionReason;
    private String reviewedBy;

    // PENDING_LIMIT 케이스 — BNKcard가 한도 산정에 사용
    private Long    estimatedMonthlyIncome;
    private Integer creditScore;
    private Integer vehicleCount;
    private Long    loanBalance;
    private Double  delinquencyRate;
    private Integer multipleDebtCount;
    private String  jobType;
}
