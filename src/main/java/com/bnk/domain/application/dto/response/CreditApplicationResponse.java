package com.bnk.domain.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class CreditApplicationResponse {
    private Long          creditAppId;
    private Long          cardId;
    private String        applicationStatus;
    private String        idVerifiedYn;
    private String        screeningResult;
    private String        limitCheckResult;
    private Long          approvedLimit;
    private Long          requestedLimit;
    private String        rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime createdAt;
}