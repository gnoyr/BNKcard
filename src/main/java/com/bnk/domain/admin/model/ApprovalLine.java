package com.bnk.domain.admin.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalLine {
    private Long approvalLineId;
    private Long approvalId;
    private Long approverAdminId;
    private String approverName;        // ADMIN_USERS JOIN
    private Integer approvalOrder;
    private String statusCode;          // PENDING / APPROVED / REJECTED
    private String commentText;
    private LocalDateTime approvedAt;
}
