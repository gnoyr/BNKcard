package com.bnk.domain.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ScreeningResultRequest {
    private Long   appId;
    private String screeningResult; // PASS / REJECTED (체크카드는 null)
    private String docVerifiedYn;   // (체크카드는 null)
    private String rejectionReason;
    private String applicationStatus; // APPROVED / REJECTED
    private String reviewedBy;		  // HOMETAX
 
    // 심사서버에서 받는 신용/재무 정보
    private Long   estimatedMonthlyIncome;  // 추정 월소득
    private Long   monthlyPayment;  		// 월 납부액 (기존 대출 상환액)
    private Integer   creditScore;          // 신용점수
    private Integer vehicleCount;           // 차량 개수
    private Long   loanBalance;             // 대출 잔액
    private Double delinquencyRate;         // 연체율
    private Integer multipleDebtCount;      // 다중채무 건수
    private String jobType;                 // REGULAR/CONTRACT/BUSINESS/FREELANCER/UNEMPLOYED/ETC
}