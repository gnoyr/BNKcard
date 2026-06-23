package com.bnk.domain.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReviewResultRequest {
    private Long   appId;
    private String applicationStatus;
    private Long   approvedLimit;
    private String rejectionReason;
    private String reviewedBy;
}