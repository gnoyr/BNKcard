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
import com.bnk.domain.card.mapper.CardMapper2;
import com.bnk.domain.card.mapper.CardStatusHistoryMapper2;
import com.bnk.domain.card.mapper.CardVersionMapper2;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model2.CardStatusHistory;
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
    private final CardStatusHistoryMapper2 cardStatusHistoryMapper2;
    private final ObjectMapper             objectMapper;

    // ─────────────────────────────────────────────────────────────
    // B-05 결재 목록 조회
    // ─────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────
    // B-06 결재 승인 (RQ-B07)
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public void approve(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {

        // 1. 결재 건 조회
        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!"PENDING".equals(approval.getStatusCode())) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DONE);
        }

        // 2. 현재 관리자 라인 APPROVED 처리
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

        // 3. 전체 라인 완료 여부 확인
        if (!approvalMapper.isAllLinesCompleted(approvalId)) {
            log.info("[결재승인] 부분 승인 완료 (대기 라인 잔존): approvalId={}, adminId={}",
                    approvalId, adminId);
            return;
        }

        // 4. version_id 기준으로 CardVersion 조회
        CardVersion cardVersion = approvalMapper.findVersionByApprovalId(approvalId);

        if (cardVersion == null) {
            // TERMS_PUBLISH 등 카드 외 결재 건
            approvalMapper.updateRequestStatus(approvalId, "APPROVED", LocalDateTime.now());
            log.info("[결재승인] 비카드 결재 완료: approvalId={}", approvalId);
            return;
        }

        Long cardId     = cardVersion.getCardId();
        String snapshot = cardVersion.getSnapshotJson();

        try {
            // 5. snapshot → model.Card 역직렬화
            Card snapshotCard = objectMapper.readValue(snapshot, Card.class);

            // 6. 현재 CARDS 상태 조회 (이력용 previousStatus)
            Card currentCard = cardMapper.findById(cardId);
            String previousStatus = (currentCard != null) ? currentCard.getCardStatus() : "DRAFT";

            // 7. snapshot 기반 CARDS 전체 필드 UPDATE
            cardMapper2.updateCard(buildCard2FromSnapshot(snapshotCard));

            // 8. CARDS.card_status = 'PUBLISHED'
            cardMapper2.updateCardStatus(cardId, "PUBLISHED");

            // 9. CARD_VERSIONS.version_status = 'PUBLISHED'
            cardVersionMapper2.updateVersionStatus(
                    cardVersion.getVersionId(), "PUBLISHED", adminId);

            // 10. CARD_STATUS_HISTORIES INSERT
            cardStatusHistoryMapper2.insertCardStatusHistory(
                    CardStatusHistory.builder()
                            .cardId(cardId)
                            .previousStatus(previousStatus)
                            .changedStatus("PUBLISHED")
                            .changedBy(adminId)
                            .changedReason("결재 승인 완료: approvalId=" + approvalId)
                            .build()
            );

            log.info("[결재승인] 전체 완료 → CARDS PUBLISHED: approvalId={}, cardId={}, versionId={}",
                    approvalId, cardId, cardVersion.getVersionId());

        } catch (JsonProcessingException e) {
            log.error("[결재승인] snapshot 역직렬화 실패: approvalId={}, versionId={}",
                    approvalId, cardVersion.getVersionId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "카드 데이터 복원 실패. 관리자에게 문의하세요.");
        }

        // 11. APPROVAL_REQUESTS.status_code = 'APPROVED'
        approvalMapper.updateRequestStatus(approvalId, "APPROVED", LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────
    // B-07 결재 반려 (RQ-B08)
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

        // 반려된 버전 ARCHIVED 처리 (target_id = version_id)
        if (approval.getTargetId() != null) {
            cardVersionMapper2.updateVersionStatus(
                    approval.getTargetId(), "ARCHIVED", adminId);
        }

        approvalMapper.updateRequestStatus(approvalId, "REJECTED", LocalDateTime.now());
        log.info("[결재반려] approvalId={}, adminId={}, versionId={}",
                approvalId, adminId, approval.getTargetId());
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helper — model.Card → model2.Card 변환
    // ─────────────────────────────────────────────────────────────
    private com.bnk.domain.card.model2.Card buildCard2FromSnapshot(Card snap) {
        return com.bnk.domain.card.model2.Card.builder()
                .cardId(snap.getCardId())
                .cardCode(snap.getCardCode())
                .cardType(snap.getCardType())
                .cardName(snap.getCardName())
                .companyName(snap.getCompanyName())
                .brandName(snap.getBrandName())
                .annualFeeDomestic(
                    snap.getAnnualFeeDomestic() != null
                        ? snap.getAnnualFeeDomestic().intValue() : null)
                .annualFeeOverseas(
                    snap.getAnnualFeeOverseas() != null
                        ? snap.getAnnualFeeOverseas().intValue() : null)
                .previousMonthSpend(snap.getPreviousMonthSpend())
                .minimumAge(snap.getMinimumAge())
                .maximumAge(snap.getMaximumAge())
                .targetUser(snap.getTargetUser())
                .summaryDescription(snap.getSummaryDescription())
                .searchableYn(snap.getSearchableYn() != null ? snap.getSearchableYn() : "Y")
                .visibleYn(snap.getVisibleYn() != null ? snap.getVisibleYn() : "Y")
                .approvalRequiredYn(snap.getApprovalRequiredYn() != null
                        ? snap.getApprovalRequiredYn() : "Y")
                .cardStatus("PUBLISHED")
                .publishStartAt(snap.getPublishStartAt())
                .publishEndAt(snap.getPublishEndAt())
                .build();
    }
}