package com.bnk.domain.application.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CheckApplicationResponse {
    private Long          checkAppId;
    private Long          cardId;
    private String        applicationStatus;
    private String        idVerifiedYn;
    private String        rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime createdAt;
}