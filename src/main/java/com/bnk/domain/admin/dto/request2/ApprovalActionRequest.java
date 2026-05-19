package com.bnk.domain.admin.dto.request2;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApprovalActionRequest {

	private Long   approvalId;    // 어떤 결재 건인지
    private String action;        // APPROVED / REJECTED
    private String commentText;   // 결재 의견 (반려 시 사유)
}
