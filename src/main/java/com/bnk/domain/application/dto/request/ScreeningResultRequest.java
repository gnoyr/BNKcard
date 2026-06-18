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
    private String applicationStatus; // APPROVED / REJECTED
    private String rejectionReason;
    private String reviewedBy;
}