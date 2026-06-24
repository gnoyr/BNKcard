package com.bnk.domain.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
public class CheckCardApplication {

    private Long   checkAppId;
    private Long   userId;
    private Long   cardId;

    private String applicationStatus;  // DRAFT/REQUESTED/APPROVED/REJECTED/ISSUED

    // STEP 2 - 본인확인
    private String idVerifiedYn;
    private String ciValue;
    
    // STEP 3
    private String applicantSnapshot;  // CLOB JSON
    
    // STEP 4 - 기본정보 + 신청정보
    private Long linkedAccountId;	   // 연결 계좌 ID
    private Long versionId;
    private String paymentSnapshot;    // CLOB JSON
    private LocalDateTime appliedAt;
    private String cardPasswordHash;

    // STEP 5 - 심사
    private String        rejectionReason;
    private LocalDateTime reviewedAt;
    private String        reviewedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}