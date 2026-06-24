package com.bnk.domain.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
public class CreditCardApplication {
    private Long   creditAppId;
    private Long   userId;
    private Long   cardId;
    private String applicationStatus;   // DRAFT/REQUESTED/REVIEWING/APPROVED/REJECTED/ISSUED
    									// REVIEWING: MANUAL_REQUIRED 케이스 추가심사 진행 중
    
    // STEP 2 - 본인확인
    private String idVerifiedYn;        // Y / N
    private String ciValue;

    // STEP 3 - 소득/신용
    private String annualIncomeBand;    // LV1/LV2/LV3/LV4
    private String creditScoreBand;     // HIGH/MID/LOW
    private Long   linkedAccountId;     // 연회비 자동이체 계좌 ID
    private String applicantSnapshot;   // CLOB JSON

    // STEP 4 - 신청정보
    private Long 		  versionId;
    private String        paymentSnapshot;  // CLOB JSON
    private Long          requestedLimit;
    private LocalDateTime appliedAt;
    private String 		  cardPasswordHash;

    // STEP 5 - 서류
    private String incomeDocKey;
    private String assetDocKey;
    private String jobDocKey;

    // STEP 6 - 1차 심사
    private String screeningResult;     // PASS / REJECTED
    private String docVerifiedYn;
    private String rejectionReason;

    // STEP 7 - 한도 검증
    private Long   estimatedMonthlyIncome;
    private String limitCheckResult;    // PASS / MANUAL_REQUIRED
    private Long   approvedLimit;

    // STEP 8 - 추가 심사
    private LocalDateTime reviewedAt;
    private String        reviewedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}