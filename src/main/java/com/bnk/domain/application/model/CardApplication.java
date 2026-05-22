package com.bnk.domain.application.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardApplication {
    private Long applicationId;
    private Long userId;
    private Long cardId;
    private String applicationStatus;   // REQUESTED/REVIEWING/APPROVED/REJECTED/ISSUED
    private String applyChannel;        // WEB / MOBILE / ADMIN
    private Long requestedLimit;
    private Long approvedLimit;
    private String rejectionReason;
    private String applicationComment;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // JOIN
    private String cardName;
    
    // 페이지 필드
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;

    public int getOffset() { return page * size; }
}
