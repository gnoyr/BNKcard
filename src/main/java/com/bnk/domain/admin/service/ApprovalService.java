package com.bnk.domain.admin.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.admin.dto.request.ApprovalActionRequest;
import com.bnk.domain.admin.dto.request.ApprovalSearchRequest;
import com.bnk.domain.admin.dto.response.ApprovalListResponse;
import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalMapper approvalMapper;
    private final CardMapper cardMapper;
    private final ObjectMapper objectMapper;

    /**
     * B-05 결재 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<ApprovalListResponse> getApprovals(ApprovalSearchRequest request) {
        long totalCount = approvalMapper.countApprovals(request);

        if (totalCount == 0) {
            return PageResponse.of(Collections.emptyList(), 0L, request.getPage(), request.getSize());
        }

        List<ApprovalRequest> approvals = approvalMapper.findApprovals(request);

        List<ApprovalListResponse> content = approvals.stream()
                .map(a -> ApprovalListResponse.builder()
                        .approvalId(a.getApprovalId())
                        .requestTypeCode(a.getRequestTypeCode())
                        .requesterName(a.getRequesterName())
                        .targetId(a.getTargetId())
                        .statusCode(a.getStatusCode())
                        .requestedAt(a.getRequestedAt())
                        .lines(a.getLines() != null
                                ? a.getLines().stream()
                                        .map(l -> ApprovalListResponse.ApprovalLineItem.builder()
                                                .approverName(l.getApproverName())
                                                .approvalOrder(l.getApprovalOrder())
                                                .statusCode(l.getStatusCode())
                                                .commentText(l.getCommentText())
                                                .approvedAt(l.getApprovedAt())
                                                .build())
                                        .collect(Collectors.toList())
                                : Collections.emptyList())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.of(content, totalCount, request.getPage(), request.getSize());
    }

    /**
     * B-06 결재 승인 (RQ-B07)
     * APPROVAL_LINES → 전체 라인 완료 확인 → CARD_VERSIONS snapshot 역직렬화 → CARDS PUBLISHED
     */
    @Transactional
    public void approve(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {
        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!"PENDING".equals(approval.getStatusCode())) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DONE);
        }

        // 현재 관리자의 라인 ID 찾기
        Long lineId = approval.getLines().stream()
                .filter(l -> adminId.equals(l.getApproverAdminId())
                        && "PENDING".equals(l.getStatusCode()))
                .map(l -> l.getApprovalLineId())
                .findFirst()
                .orElse(null);

        if (lineId != null) {
            approvalMapper.updateLineStatus(lineId, "APPROVED",
                    request.getComment(), LocalDateTime.now());
        }

        // 전체 라인 완료 여부 확인
        if (approvalMapper.isAllLinesCompleted(approvalId)) {
            // snapshot_json 역직렬화 → CARDS UPDATE → PUBLISHED
            String snapshotJson = approvalMapper.findVersionSnapshot(approvalId);
            if (snapshotJson != null && approval.getTargetId() != null) {
                cardMapper.updateCardFromSnapshot(approval.getTargetId(), snapshotJson);
                cardMapper.updateCardStatus(approval.getTargetId(), "PUBLISHED");
            }
            approvalMapper.updateRequestStatus(approvalId, "APPROVED", LocalDateTime.now());
            log.info("[결재승인] 전체 완료 → CARDS PUBLISHED: approvalId={}, cardId={}",
                    approvalId, approval.getTargetId());
        }
    }

    /**
     * B-07 결재 반려 (RQ-B08)
     * comment 필수, CARDS.card_status 변경 없음(DRAFT 유지)
     */
    @Transactional
    public void reject(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {
        if (request.getComment() == null || request.getComment().isBlank()) {
            throw new BusinessException(ErrorCode.REJECT_COMMENT_REQUIRED);
        }

        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!"PENDING".equals(approval.getStatusCode())) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DONE);
        }

        // 현재 관리자의 라인 반려 처리
        Long lineId = approval.getLines().stream()
                .filter(l -> adminId.equals(l.getApproverAdminId())
                        && "PENDING".equals(l.getStatusCode()))
                .map(l -> l.getApprovalLineId())
                .findFirst()
                .orElse(null);

        if (lineId != null) {
            approvalMapper.updateLineStatus(lineId, "REJECTED",
                    request.getComment(), LocalDateTime.now());
        }

        // APPROVAL_REQUESTS → REJECTED (CARDS.card_status는 DRAFT 유지)
        approvalMapper.updateRequestStatus(approvalId, "REJECTED", LocalDateTime.now());
        log.info("[결재반려] approvalId={}, adminId={}", approvalId, adminId);
    }
}