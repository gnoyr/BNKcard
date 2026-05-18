package com.bnk.domain.admin.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {
    private Long approvalId;
    private String requestTypeCode;     // CARD_PUBLISH / CARD_UPDATE / TERMS_PUBLISH
    private Long requesterAdminId;
    private String requesterName;       // ADMIN_USERS JOIN
    private Long targetId;
    private String statusCode;          // PENDING / APPROVED / REJECTED
    private String requestComment;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private List<ApprovalLine> lines;
}
