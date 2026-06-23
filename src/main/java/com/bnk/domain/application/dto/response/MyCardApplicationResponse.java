package com.bnk.domain.application.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MyCardApplicationResponse {
    private Long           appId;
    private String         cardType;	// CREDIT / CHECK
    private Long           cardId;
    private String         cardName;
    private String         applicationStatus;
    private String         rejectionReason;
    private LocalDateTime  appliedAt;
    private LocalDateTime  createdAt;
}