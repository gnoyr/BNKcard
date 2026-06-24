package com.bnk.domain.application.dto.response;

import java.time.LocalDateTime;

import com.bnk.domain.application.dto.CheckApplicantSnapshotDto;
import com.bnk.domain.application.dto.PaymentSnapshotDto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CheckApplicationResponse {
    private Long checkAppId;
    private Long cardId;
    private String cardName;
    private String cardImageUrl;
    private String applicationStatus;
    private CheckApplicantSnapshotDto  applicantSnapshot;
    private PaymentSnapshotDto paymentSnapshot;
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime createdAt;
}