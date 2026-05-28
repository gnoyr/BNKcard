package com.bnk.domain.admin.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.admin.dto.request.ApprovalSearchRequest;
import com.bnk.domain.admin.model.ApprovalLine;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.model.CardVersion;

@Mapper
public interface ApprovalMapper {

    Optional<ApprovalRequest> findById(@Param("approvalId") Long approvalId);

    List<ApprovalRequest> findApprovals(ApprovalSearchRequest request);

    long countApprovals(ApprovalSearchRequest request);

    int insertApprovalRequest(ApprovalRequest approvalRequest);

    int insertApprovalLine(ApprovalLine approvalLine);

    int updateLineStatus(@Param("approvalLineId") Long approvalLineId,
                         @Param("statusCode") String statusCode,
                         @Param("commentText") String commentText,
                         @Param("approvedAt") LocalDateTime approvedAt);

    int updateRequestStatus(@Param("approvalId") Long approvalId,
                            @Param("statusCode") String statusCode,
                            @Param("completedAt") LocalDateTime completedAt);

    boolean isAllLinesCompleted(@Param("approvalId") Long approvalId);

    /** CARD_VERSIONS.snapshot_json 조회 — 결재 승인 후 CARDS 복원용 */
    String findVersionSnapshot(@Param("approvalId") Long approvalId);

    int countPendingApprovals();
    
    /** 결재 건의 version_id 기준으로 CardVersion 조회 */
    CardVersion findVersionByApprovalId(@Param("approvalId") Long approvalId);
}
