package com.bnk.domain.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class CheckCardApplication {

    private Long   checkAppId;
    private Long   userId;
    private Long   cardId;

    private String applicationStatus;  // DRAFT/REQUESTED/APPROVED/REJECTED/ISSUED

    // STEP 2 - 본인확인
    private String idVerifiedYn;
    
    // STEP 3
    private Long linkedAccountId;	   // 연결 계좌 ID
    
    // STEP 4 - 기본정보 + 신청정보
    private String applicantSnapshot;  // CLOB JSON
    private String paymentSnapshot;    // CLOB JSON
    private LocalDateTime appliedAt;

    // STEP 5 - 심사
    private String        rejectionReason;
    private LocalDateTime reviewedAt;
    private String        reviewedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}