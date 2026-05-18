package com.bnk.domain.application.dto.response;

import com.bnk.domain.application.model.CardApplication;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApplicationResponse {
    private Long applicationId;
    private Long cardId;
    private String cardName;
    private String applicationStatus;
    private String applyChannel;
    private Long requestedLimit;
    private Long approvedLimit;
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;

    public static ApplicationResponse from(CardApplication ca) {
        return ApplicationResponse.builder()
                .applicationId(ca.getApplicationId())
                .cardId(ca.getCardId())
                .cardName(ca.getCardName())
                .applicationStatus(ca.getApplicationStatus())
                .applyChannel(ca.getApplyChannel())
                .requestedLimit(ca.getRequestedLimit())
                .approvedLimit(ca.getApprovedLimit())
                .rejectionReason(ca.getRejectionReason())
                .appliedAt(ca.getAppliedAt())
                .reviewedAt(ca.getReviewedAt())
                .build();
    }
}
