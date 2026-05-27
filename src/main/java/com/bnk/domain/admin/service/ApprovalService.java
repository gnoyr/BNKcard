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
import com.bnk.domain.admin.dto.response.ApprovalDetailResponse;
import com.bnk.domain.admin.dto.response.ApprovalListResponse;
import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.dto.request.CardSnapshot;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.mapper.CardMapper2;
import com.bnk.domain.card.mapper.CardStatusHistoryMapper;
import com.bnk.domain.card.mapper.CardVersionMapper2;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardStatusHistory;
import com.bnk.domain.card.model2.CardVersion;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalMapper           approvalMapper;
    private final CardMapper               cardMapper;
    private final CardMapper2              cardMapper2;
    private final CardVersionMapper2       cardVersionMapper2;
    private final CardStatusHistoryMapper cardStatusHistoryMapper;
    private final ObjectMapper             objectMapper;

    // ─────────────────────────────────────────────────────────────
    // 결재 목록 조회 — 현재 로그인 관리자 할당 건만
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<ApprovalListResponse> getApprovals(
            ApprovalSearchRequest request, Long adminId) {

        // 현재 로그인 관리자 ID를 필터로 주입
        request.setApproverAdminId(adminId);

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
                        .targetName(a.getTargetName())       // 추가
                        .statusCode(a.getStatusCode())
                        .requestedAt(a.getRequestedAt())
                        .lines(a.getLines() != null
                                ? a.getLines().stream()
                                        .map(l -> ApprovalListResponse.ApprovalLineItem.builder()
                                                .approvalLineId(l.getApprovalLineId())
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

    // ─────────────────────────────────────────────────────────────
    // 결재 상세 조회 — 라인 + 현재 관리자 처리 가능 여부 포함
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ApprovalDetailResponse getApprovalDetail(Long approvalId, Long adminId) {

        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        List<ApprovalDetailResponse.LineItem> lineItems = approval.getLines() != null
                ? approval.getLines().stream()
                        .map(l -> ApprovalDetailResponse.LineItem.builder()
                                .approvalLineId(l.getApprovalLineId())
                                .approvalOrder(l.getApprovalOrder())
                                .approverName(l.getApproverName())
                                .statusCode(l.getStatusCode())
                                .commentText(l.getCommentText())
                                .approvedAt(l.getApprovedAt())
                                // 현재 로그인 관리자이고 아직 PENDING 상태인 라인만 처리 가능
                                .isCurrentUser(adminId.equals(l.getApproverAdminId())
                                        && "PENDING".equals(l.getStatusCode()))
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();
        
        CardSnapshot snapshotInfo = null;
        if (approval.getTargetId() != null && approval.getRequestTypeCode().startsWith("CARD_")) {
            CardVersion cardVersion = cardVersionMapper2.getCardVersion(approval.getTargetId());
            if (cardVersion != null && cardVersion.getSnapshotJson() != null) {
                try {
                    snapshotInfo = objectMapper.readValue(
                            cardVersion.getSnapshotJson(), CardSnapshot.class);
                } catch (JsonProcessingException e) {
                    log.warn("[결재상세] snapshot 파싱 실패: approvalId={}, versionId={}",
                            approvalId, approval.getTargetId(), e);
                }
            }
        }

        return ApprovalDetailResponse.builder()
                .approvalId(approval.getApprovalId())
                .requestTypeCode(approval.getRequestTypeCode())
                .requesterName(approval.getRequesterName())
                .targetId(approval.getTargetId())
                .targetName(approval.getTargetName())
                .statusCode(approval.getStatusCode())
                .requestComment(approval.getRequestComment())
                .requestedAt(approval.getRequestedAt())
                .completedAt(approval.getCompletedAt())
                .lines(lineItems)
                .snapshot(snapshotInfo)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 결재 승인
    // 변경: 전체 라인 완료 시 PUBLISHED 대신 APPROVED로 변경
    //       실제 PUBLISHED 전환은 스케줄러(CardScheduler)가 처리
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public void approve(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {

        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!"PENDING".equals(approval.getStatusCode())) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DONE);
        }

        // 현재 관리자 라인 APPROVED 처리
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
        if (!approvalMapper.isAllLinesCompleted(approvalId)) {
            log.info("[결재승인] 부분 승인 완료 (대기 라인 잔존): approvalId={}, adminId={}",
                    approvalId, adminId);
            return;
        }

        // version_id 기준 CardVersion 조회
        CardVersion cardVersion = approvalMapper.findVersionByApprovalId(approvalId);

        if (cardVersion == null) {
            // TERMS_PUBLISH 등 카드 외 결재건
            approvalMapper.updateRequestStatus(approvalId, "APPROVED", LocalDateTime.now());
            log.info("[결재승인] 비카드 결재 완료: approvalId={}", approvalId);
            return;
        }

        Long cardId     = cardVersion.getCardId();
        String snapshot = cardVersion.getSnapshotJson();

        try {
            Card snapshotCard = objectMapper.readValue(snapshot, Card.class);

            Card currentCard = cardMapper.findById(cardId);
            String previousStatus = (currentCard != null) ? currentCard.getCardStatus() : "DRAFT";

            // snapshot 기반 CARDS 전체 필드 UPDATE
            cardMapper2.updateCard(buildCard2FromSnapshot(snapshotCard));

            // ── 핵심 변경 ───────────────────────────────────────────
            // 수정 전: CARDS.card_status = 'PUBLISHED' (즉시 게시)
            // 수정 후: CARDS.card_status = 'APPROVED'  (게시 대기)
            //          스케줄러가 publish_start_at 도달 시 PUBLISHED로 전환
            cardMapper2.updateCardStatus(cardId, "APPROVED");
            // ────────────────────────────────────────────────────────

            // CARD_VERSIONS.version_status = 'APPROVED'
            cardVersionMapper2.updateVersionStatus(
                    cardVersion.getVersionId(), "APPROVED", adminId);

            // 이력 기록
            cardStatusHistoryMapper.insertCardStatusHistory(
                    CardStatusHistory.builder()
                            .cardId(cardId)
                            .previousStatus(previousStatus)
                            .changedStatus("APPROVED")
                            .changedBy(adminId)
                            .changedReason("결재 승인 완료: approvalId=" + approvalId)
                            .build()
            );

            log.info("[결재승인] 전체 완료 → CARDS APPROVED (게시 대기): approvalId={}, cardId={}, versionId={}",
                    approvalId, cardId, cardVersion.getVersionId());

        } catch (JsonProcessingException e) {
            log.error("[결재승인] snapshot 역직렬화 실패: approvalId={}, versionId={}",
                    approvalId, cardVersion.getVersionId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "카드 데이터 복원 실패. 관리자에게 문의하세요.");
        }

        approvalMapper.updateRequestStatus(approvalId, "APPROVED", LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────
    // 결재 반려
    // ─────────────────────────────────────────────────────────────
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

        // 반려된 버전 ARCHIVED 처리
        if (approval.getTargetId() != null) {
            cardVersionMapper2.updateVersionStatus(
                    approval.getTargetId(), "ARCHIVED", adminId);
        }

        approvalMapper.updateRequestStatus(approvalId, "REJECTED", LocalDateTime.now());
        log.info("[결재반려] approvalId={}, adminId={}, versionId={}",
                approvalId, adminId, approval.getTargetId());
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helper
    // ─────────────────────────────────────────────────────────────
    private com.bnk.domain.card.model2.Card buildCard2FromSnapshot(Card snap) {
        return com.bnk.domain.card.model2.Card.builder()
                .cardId(snap.getCardId())
                .cardCode(snap.getCardCode())
                .cardType(snap.getCardType())
                .cardName(snap.getCardName())
                .companyName(snap.getCompanyName())
                .brandName(snap.getBrandName())
                .annualFeeDomestic(snap.getAnnualFeeDomestic())
                .annualFeeOverseas(snap.getAnnualFeeOverseas())
                .previousMonthSpend(snap.getPreviousMonthSpend())
                .minimumAge(snap.getMinimumAge())
                .maximumAge(snap.getMaximumAge())
                .targetUser(snap.getTargetUser())
                .summaryDescription(snap.getSummaryDescription())
                .searchableYn(snap.getSearchableYn() != null ? snap.getSearchableYn() : "Y")
                .visibleYn(snap.getVisibleYn() != null ? snap.getVisibleYn() : "Y")
                .approvalRequiredYn(snap.getApprovalRequiredYn() != null
                        ? snap.getApprovalRequiredYn() : "Y")
                .cardStatus("APPROVED")   // 결재 승인 → APPROVED (스케줄러가 PUBLISHED 전환)
                .publishStartAt(snap.getPublishStartAt())
                .publishEndAt(snap.getPublishEndAt())
                .build();
    }
}