package com.bnk.domain.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.admin.model.ApprovalLine;
import com.bnk.domain.admin.model.ApprovalRequest;

@Mapper
public interface ApprovalMapper2 {
	void insertApprovalRequest(ApprovalRequest approvalRequest);
    void insertApprovalLine(ApprovalLine approvalLine);

    // 결재 신청서 상태 변경 (PENDING → APPROVED / REJECTED)
    void updateApprovalStatus(@Param("approvalId") Long approvalId,
                              @Param("statusCode") String statusCode);

    // 승인 라인 상태 변경 (PENDING → APPROVED / REJECTED)
    void updateApprovalLineStatus(@Param("approvalLineId") Long approvalLineId,
                                  @Param("statusCode") String statusCode,
                                  @Param("commentText") String commentText);
}
