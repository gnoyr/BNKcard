package com.bnk.domain.admin.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.admin.dto.request.ApprovalActionRequest;
import com.bnk.domain.admin.dto.request.ApprovalSearchRequest;
import com.bnk.domain.admin.dto.response.ApprovalDetailResponse;
import com.bnk.domain.admin.dto.response.ApprovalListResponse;
import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalLine;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.dto.CardSnapshot;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.mapper.CardStatusHistoryMapper;
import com.bnk.domain.card.mapper.CardVersionMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardStatusHistory;
import com.bnk.domain.card.model.CardVersion;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.log.annotation.Loggable;
import com.bnk.global.response.PageResponse;
import com.bnk.global.util.audit.AuditLogger;
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

	private final ApprovalMapper approvalMapper;
	private final CardMapper cardMapper;
	private final CardVersionMapper cardVersionMapper;
	private final CardStatusHistoryMapper cardStatusHistoryMapper;
	private final ObjectMapper objectMapper;
	private final TermsMapper termsMapper;
	private final AuditLogger auditLogger;

	private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

	// ── 결재 상태 코드 ────────────────────────────────────────────────
	private static final String STATUS_PENDING = "PENDING";
	private static final String STATUS_APPROVED = "APPROVED";
	private static final String STATUS_REJECTED = "REJECTED";
	private static final String STATUS_ARCHIVED = "ARCHIVED";

	// ── 카드/약관 상태 코드 ───────────────────────────────────────────
	private static final String CARD_STATUS_DRAFT = "DRAFT";
	private static final String CARD_STATUS_REVIEW = "REVIEW";
	private static final String CARD_STATUS_APPROVED = "APPROVED";
	private static final String TERMS_STATUS_DRAFT = "DRAFT";
	private static final String TERMS_STATUS_PUBLISHED = "PUBLISHED";

	// ── 결재 유형 코드 ────────────────────────────────────────────────
	private static final String TYPE_TERMS_PUBLISH = "TERMS_PUBLISH";
	private static final String TYPE_CARD_PUBLISH = "CARD_PUBLISH";
	private static final String TYPE_CARD_UPDATE = "CARD_UPDATE";
	private static final String TYPE_CARD_PREFIX = "CARD_";

    // ─────────────────────────────────────────────────────────────
    // 결재 목록 조회
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<ApprovalListResponse> getApprovals(
            @Valid ApprovalSearchRequest request, Long adminId) {

        request.setApproverAdminId(adminId);

        long totalCount = approvalMapper.countApprovals(request);
        if (totalCount == 0) {
            return PageResponse.of(Collections.emptyList(), 0L,
                    request.getPage(), request.getSize());
        }

        List<ApprovalRequest> approvals = approvalMapper.findApprovals(request);
        List<ApprovalListResponse> content = approvals.stream()
                .map(a -> ApprovalListResponse.builder()
                        .approvalId(a.getApprovalId())
                        .requestTypeCode(a.getRequestTypeCode())
                        .requesterName(a.getRequesterName())
                        .targetId(a.getTargetId())
                        .targetName(a.getTargetName())
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
    // 결재 상세 조회
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ApprovalDetailResponse getApprovalDetail(Long approvalId, Long adminId) {

        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        List<ApprovalDetailResponse.ApprovalLineItem> lineItems = approval.getLines() != null
                ? approval.getLines().stream()
                        .map(l -> ApprovalDetailResponse.ApprovalLineItem.builder()
                                .approvalLineId(l.getApprovalLineId())
                                .approvalOrder(l.getApprovalOrder())
                                .approverName(l.getApproverName())
                                .statusCode(l.getStatusCode())
                                .commentText(l.getCommentText())
                                .approvedAt(l.getApprovedAt())
                                .isCurrentUser(adminId.equals(l.getApproverAdminId())
                                        && STATUS_PENDING.equals(l.getStatusCode()))
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        // CARD 결재 건이면 snapshot 파싱
        CardSnapshot snapshotInfo = null;
        if (approval.getTargetId() != null
                && approval.getRequestTypeCode() != null
                && approval.getRequestTypeCode().startsWith(TYPE_CARD_PREFIX)) {
            CardVersion cardVersion = cardVersionMapper.getCardVersion(approval.getTargetId());
            if (cardVersion != null && cardVersion.getSnapshotJson() != null) {
                try {
                    snapshotInfo = objectMapper.readValue(
                            cardVersion.getSnapshotJson(), CardSnapshot.class);
                } catch (JsonProcessingException e) {
                    auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_REQUEST,
                            adminId, String.valueOf(approvalId), "snapshot 파싱 실패: versionId=" + approval.getTargetId());
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
    // ─────────────────────────────────────────────────────────────
    @Transactional
    @Loggable(eventType = "APPROVAL_APPROVE", targetType = "APPROVAL", actionDetail = "결재승인")
    public void approve(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {

        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseGet(() -> {
                    auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                            adminId, String.valueOf(approvalId), "결재 건 없음");
                    throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
                });

        if (!STATUS_PENDING.equals(approval.getStatusCode())) {
            auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                    adminId, String.valueOf(approvalId), "이미 처리된 결재");
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DONE);
        }

        // 내 결재 라인 승인 처리
        Long lineId = approval.getLines().stream()
                .filter(l -> adminId.equals(l.getApproverAdminId())
                        && STATUS_PENDING.equals(l.getStatusCode()))
                .map(ApprovalLine::getApprovalLineId)
                .findFirst()
                .orElse(null);

        if (lineId != null) {
            approvalMapper.updateLineStatus(lineId, STATUS_APPROVED,
                    request.getComment(), LocalDateTime.now(KST_ZONE));
        }

        // 전체 라인 완료 여부 확인
        if (!approvalMapper.isAllLinesCompleted(approvalId)) {
            auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                    adminId, String.valueOf(approvalId), "부분 승인 완료 (대기 라인 잔존)");
            return;
        }

        // ── 분기: 결재 유형에 따라 처리 ─────────────────────────────────
        String typeCode = approval.getRequestTypeCode();

        if (TYPE_TERMS_PUBLISH.equals(typeCode)) {
            handleTermsApprove(approval, adminId);
        } else if (TYPE_CARD_PUBLISH.equals(typeCode) || TYPE_CARD_UPDATE.equals(typeCode)) {
            handleCardApprove(approval, adminId, approvalId);
        }

        approvalMapper.updateRequestStatus(approvalId, STATUS_APPROVED, LocalDateTime.now(KST_ZONE));
        auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                adminId, String.valueOf(approvalId), "결재 승인 완료: typeCode=" + typeCode);
    }

    // ── 약관 승인 처리 ──────────────────────────────────────────────
    private void handleTermsApprove(ApprovalRequest approval, Long adminId) {
        Long termsId = approval.getTargetId();
        if (termsId == null) return;

        Terms terms = termsMapper.findById(termsId).orElse(null);
        if (terms == null) {
            auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                    adminId, String.valueOf(termsId), "약관 데이터 없음");
            return;
        }

        String previousStatus = terms.getStatus();
        
        // 1. TERMS status → PUBLISHED
        termsMapper.updateTermsStatus(termsId, TERMS_STATUS_PUBLISHED, adminId);

        // 2. TERMS_STATUS_HISTORY INSERT
        termsMapper.insertStatusHistory(
                termsId, previousStatus, TERMS_STATUS_PUBLISHED, adminId,
                "결재 승인 완료: approvalId=" + approval.getApprovalId());

        // 3. 같은 terms_master_id의 기존 PUBLISHED → SUPERSEDED 처리
        termsMapper.supersedePreviousPublished(terms.getTermsMasterId(), termsId);

        // 4. 재동의 알림
        if ("Y".equals(terms.getReconsentRequiredYn())) {
            List<Long> userIds = termsMapper.findUserIdsForReconsent(termsId);
            userIds.forEach(uid ->
                    termsMapper.insertNotificationHistory(termsId, uid, "EMAIL"));
            auditLogger.adminSuccess(AuditLogger.TERMS, AuditLogger.AGREE,
                    adminId, String.valueOf(termsId), "재동의 알림 발송: " + userIds.size() + "명");
        }

        auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                adminId, String.valueOf(termsId), "약관 결재 승인 완료: " + previousStatus + " → PUBLISHED");
    }

    // ── 카드 승인 처리 ──────────────────────────────────────────────
    private void handleCardApprove(ApprovalRequest approval, Long adminId, Long approvalId) {
        CardVersion cardVersion = approvalMapper.findVersionByApprovalId(approvalId);
        if (cardVersion == null) {
            auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                    adminId, String.valueOf(approvalId), "CardVersion 없음");
            return;
        }

        Long cardId   = cardVersion.getCardId();
        String snapshot = cardVersion.getSnapshotJson();

        try {
            CardSnapshot snap = objectMapper.readValue(snapshot, CardSnapshot.class);
            Card snapshotCard = snap.getCard();

            String previousStatus = Optional.ofNullable(cardMapper.findById(cardId))
                    .map(Card::getCardStatus)
                    .orElse(CARD_STATUS_DRAFT);

            Card approvedCard = Card.builder()
                    .cardId(snapshotCard.getCardId())
                    .cardCode(snapshotCard.getCardCode())
                    .cardType(snapshotCard.getCardType())
                    .cardName(snapshotCard.getCardName())
                    .companyName(snapshotCard.getCompanyName())
                    .companyCode(snapshotCard.getCompanyCode())
                    .brandName(snapshotCard.getBrandName())
                    .annualFeeDomestic(snapshotCard.getAnnualFeeDomestic())
                    .annualFeeOverseas(snapshotCard.getAnnualFeeOverseas())
                    .previousMonthSpend(snapshotCard.getPreviousMonthSpend())
                    .minimumAge(snapshotCard.getMinimumAge())
                    .maximumAge(snapshotCard.getMaximumAge())
                    .creditLimitMin(snapshotCard.getCreditLimitMin())
                    .creditLimitMax(snapshotCard.getCreditLimitMax())
                    .targetUser(snapshotCard.getTargetUser())
                    .summaryDescription(snapshotCard.getSummaryDescription())
                    .searchableYn(snapshotCard.getSearchableYn() != null
                            ? snapshotCard.getSearchableYn() : "Y")
                    .visibleYn(snapshotCard.getVisibleYn() != null
                            ? snapshotCard.getVisibleYn() : "Y")
                    .approvalRequiredYn(snapshotCard.getApprovalRequiredYn() != null
                            ? snapshotCard.getApprovalRequiredYn() : "Y")
                    .cardStatus(CARD_STATUS_APPROVED)
                    .publishStartAt(snapshotCard.getPublishStartAt())
                    .publishEndAt(snapshotCard.getPublishEndAt())
                    .updatedBy(adminId)
                    .build();

            cardMapper.updateCard(approvedCard);
            cardMapper.updateCardStatus(cardId, CARD_STATUS_APPROVED);

            cardVersionMapper.updateVersionStatus(
                    cardVersion.getVersionId(), STATUS_APPROVED, adminId);

            cardStatusHistoryMapper.insertCardStatusHistory(
                    CardStatusHistory.builder()
                            .cardId(cardId)
                            .previousStatus(previousStatus)
                            .changedStatus(CARD_STATUS_APPROVED)
                            .changedBy(adminId)
                            .changedReason("결재 승인 완료: approvalId=" + approvalId)
                            .build());

            auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                    adminId, String.valueOf(cardId), "카드 결재 승인 완료: approvalId=" + approvalId);

        } catch (JsonProcessingException e) {
            auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_APPROVE,
                    adminId, String.valueOf(approvalId), "snapshot 역직렬화 실패: " + e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "카드 데이터 복원 실패. 관리자에게 문의하세요.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 결재 반려
    // ─────────────────────────────────────────────────────────────
    @Transactional
    @Loggable(eventType = "APPROVAL_REJECT", targetType = "APPROVAL", actionDetail = "결재반려")
    public void reject(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {

        if (request.getComment() == null || request.getComment().isBlank()) {
            auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_REJECT,
                    adminId, String.valueOf(approvalId), "반려 사유 누락");
            throw new BusinessException(ErrorCode.REJECT_COMMENT_REQUIRED);
        }

        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseGet(() -> {
                    auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_REJECT,
                            adminId, String.valueOf(approvalId), "결재 건 없음");
                    throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
                });

        if (!STATUS_PENDING.equals(approval.getStatusCode())) {
            auditLogger.adminFailure(AuditLogger.CARD, AuditLogger.APPROVAL_REJECT,
                    adminId, String.valueOf(approvalId), "이미 처리된 결재");
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DONE);
        }

        // 내 결재 라인 반려 처리
        Long lineId = approval.getLines().stream()
                .filter(l -> adminId.equals(l.getApproverAdminId())
                        && STATUS_PENDING.equals(l.getStatusCode()))
                .map(ApprovalLine::getApprovalLineId)
                .findFirst()
                .orElse(null);

        if (lineId != null) {
            approvalMapper.updateLineStatus(lineId, STATUS_REJECTED,
                    request.getComment(), LocalDateTime.now(KST_ZONE));
        }

        // ── 분기: 결재 유형에 따라 처리 ─────────────────────────────────
        String typeCode = approval.getRequestTypeCode();

        if (TYPE_TERMS_PUBLISH.equals(typeCode)) {
            handleTermsReject(approval, adminId);
        } else if (TYPE_CARD_PUBLISH.equals(typeCode) || TYPE_CARD_UPDATE.equals(typeCode)) {
            handleCardReject(approval, adminId, approvalId);
        }

        approvalMapper.updateRequestStatus(approvalId, STATUS_REJECTED, LocalDateTime.now(KST_ZONE));
        auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_REJECT,
                adminId, String.valueOf(approvalId), "결재 반려 완료: typeCode=" + typeCode);
    }

    // ── 약관 반려 처리 ──────────────────────────────────────────────
    private void handleTermsReject(ApprovalRequest approval, Long adminId) {
        Long termsId = approval.getTargetId();
        if (termsId == null) return;

        Terms terms = termsMapper.findById(termsId).orElse(null);
        if (terms == null) return;

        String previousStatus = terms.getStatus();

        // REJECTED → DRAFT (반려 시 재검토 상태로)
        termsMapper.updateTermsStatus(termsId, TERMS_STATUS_DRAFT, adminId);

        termsMapper.insertStatusHistory(
                termsId, previousStatus, TERMS_STATUS_DRAFT, adminId,
                "결재 반려: approvalId=" + approval.getApprovalId());

        auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_REJECT,
                adminId, String.valueOf(termsId), "약관 결재 반려 완료: " + previousStatus + " → DRAFT");
    }

    // ── 카드 반려 처리 ──────────────────────────────────────────────
    private void handleCardReject(ApprovalRequest approval, Long adminId, Long approvalId) {
        if (approval.getTargetId() == null) return;

        CardVersion cardVersion = approvalMapper.findVersionByApprovalId(approvalId);

        // CARD_VERSIONS → ARCHIVED
        cardVersionMapper.updateVersionStatus(approval.getTargetId(), STATUS_ARCHIVED, adminId);

        if (cardVersion != null && cardVersion.getCardId() != null) {
            String previousStatus = Optional.ofNullable(
                    cardMapper.findById(cardVersion.getCardId()))
                    .map(Card::getCardStatus)
                    .orElse(CARD_STATUS_REVIEW); 

            // CARDS → REVIEW
            cardMapper.updateCardStatus(cardVersion.getCardId(), CARD_STATUS_REVIEW);

            cardStatusHistoryMapper.insertCardStatusHistory(
                    CardStatusHistory.builder()
                            .cardId(cardVersion.getCardId())
                            .previousStatus(previousStatus)
                            .changedStatus(CARD_STATUS_REVIEW)
                            .changedBy(adminId)
                            .changedReason("결재 반려: approvalId=" + approvalId)
                            .build());

            auditLogger.adminSuccess(AuditLogger.CARD, AuditLogger.APPROVAL_REJECT,
                    adminId, String.valueOf(cardVersion.getCardId()), "카드 결재 반려 완료: approvalId=" + approvalId);
        }
    }
}