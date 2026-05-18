package com.bnk.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ApprovalListResponse {
    private Long approvalId;
    private String requestTypeCode;
    private String requesterName;
    private Long targetId;
    private String statusCode;
    private LocalDateTime requestedAt;
    private List<ApprovalLineItem> lines;

    @Getter
    @Builder
    public static class ApprovalLineItem {
        private String approverName;
        private Integer approvalOrder;
        private String statusCode;
        private String commentText;
        private LocalDateTime approvedAt;
    }
}
