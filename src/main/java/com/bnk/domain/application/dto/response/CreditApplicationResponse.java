package com.bnk.domain.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

import com.bnk.domain.application.dto.CreditApplicantSnapshotDto;
import com.bnk.domain.application.dto.PaymentSnapshotDto;

@Getter @Builder
public class CreditApplicationResponse {
    private Long creditAppId;
    private Long cardId;
    private String cardName;
    private String cardImageUrl;
    private String applicationStatus;
    private CreditApplicantSnapshotDto applicantSnapshot;
    private PaymentSnapshotDto paymentSnapshot;
    private Long approvedLimit;
    private Long requestedLimit;
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime createdAt;
}