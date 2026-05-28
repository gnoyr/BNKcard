package com.bnk.domain.admin.service;

import java.time.LocalDateTime;
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
import com.bnk.domain.card.dto.request.CardSnapshot;
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
import com.bnk.global.response.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결재 서비스 (리팩토링)
 *
 *    → 역직렬화된 model.Card를 cardMapper.updateCard()에 직접 전달
 *  - @JsonAlias("updateBy")가 Card에 선언되어 있으므로
 *    기존 스냅샷 JSON(updateBy 키) 역직렬화도 정상 동작
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalMapper          approvalMapper;
    private final CardMapper              cardMapper;             
    private final CardVersionMapper       cardVersionMapper;
    private final CardStatusHistoryMapper cardStatusHistoryMapper;
    private final ObjectMapper            objectMapper;
    private final TermsMapper             termsMapper;

    // ─────────────────────────────────────────────────────────────
    // 결재 목록 조회
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<ApprovalListResponse> getApprovals(
            @Valid ApprovalSearchRequest request, Long adminId) {

    	// 현재 로그인 관리자 ID를 필터로 주입 (내 할당 건만 조회)
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
                        .requesterName(a.getRequesterName())        // ← requesterAdminId → requesterName
                        .targetId(a.getTargetId())
                        .targetName(a.getTargetName())              // ← 카드명 or 약관명
                        .statusCode(a.getStatusCode())
                        .requestedAt(a.getRequestedAt())
                        .lines(a.getLines() != null                 // ← 라인 서브매핑 추가
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
    public ApprovalDetailResponse getApprovalDetail(Long approvalId, Long adminId) {  // ← adminId 추가
        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        // 라인 서브매핑 — isCurrentUser: 내 차례(PENDING 상태) 판별
        List<ApprovalDetailResponse.LineItem> lineItems = approval.getLines() != null
                ? approval.getLines().stream()
                        .map(l -> ApprovalDetailResponse.LineItem.builder()
                                .approvalLineId(l.getApprovalLineId())
                                .approvalOrder(l.getApprovalOrder())
                                .approverName(l.getApproverName())
                                .statusCode(l.getStatusCode())
                                .commentText(l.getCommentText())
                                .approvedAt(l.getApprovedAt())
                                .isCurrentUser(adminId.equals(l.getApproverAdminId())
                                        && "PENDING".equals(l.getStatusCode()))
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        // CARD 결재 건이면 스냅샷 파싱 (카드 수정 내용 미리보기용)
        com.bnk.domain.card.dto.request.CardSnapshot snapshotInfo = null;
        if (approval.getTargetId() != null
                && approval.getRequestTypeCode() != null
                && approval.getRequestTypeCode().startsWith("CARD_")) {
            CardVersion cardVersion = cardVersionMapper.getCardVersion(approval.getTargetId());
            if (cardVersion != null && cardVersion.getSnapshotJson() != null) {
                try {
                    snapshotInfo = objectMapper.readValue(
                            cardVersion.getSnapshotJson(),
                            com.bnk.domain.card.dto.request.CardSnapshot.class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    log.warn("[결재상세] snapshot 파싱 실패: approvalId={}, versionId={}",
                            approvalId, approval.getTargetId(), e);
                }
            }
        }

        return ApprovalDetailResponse.builder()
                .approvalId(approval.getApprovalId())
                .requestTypeCode(approval.getRequestTypeCode())
                .requesterName(approval.getRequesterName())          // ← requesterAdminId → requesterName
                .targetId(approval.getTargetId())
                .targetName(approval.getTargetName())                // ← 카드명 or 약관명
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
    public void approve(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {

        ApprovalRequest approval = approvalMapper.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!"PENDING".equals(approval.getStatusCode())) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DONE);
        }

        Long lineId = approval.getLines().stream()
                .filter(l -> adminId.equals(l.getApproverAdminId())
                        && "PENDING".equals(l.getStatusCode()))
                .map(ApprovalLine::getApprovalLineId)
                .findFirst()
                .orElse(null);

        if (lineId != null) {
            approvalMapper.updateLineStatus(lineId, "APPROVED",
                    request.getComment(), LocalDateTime.now());
        }

        if (!approvalMapper.isAllLinesCompleted(approvalId)) {
            log.info("[결재승인] 부분 승인 완료 (대기 라인 잔존): approvalId={}, adminId={}",
                    approvalId, adminId);
            return;
        }

        // ── TERMS_PUBLISH 승인 ───────────────────────────────────
        if ("TERMS_PUBLISH".equals(approval.getRequestTypeCode())) {
            Long termsId = approval.getTargetId();
            if (termsId != null) {
                Terms terms = termsMapper.findById(termsId).orElse(null);
                if (terms != null) {
                    String previousStatus = terms.getStatus();
                    termsMapper.updateTermsStatus(termsId, "APPROVED", adminId);
                    termsMapper.insertStatusHistory(
                            termsId, previousStatus, "APPROVED", adminId,
                            "결재 승인 완료: approvalId=" + approvalId);
                    log.info("[결재승인] 약관 상태 → APPROVED: approvalId={}, termsId={}", approvalId, termsId);
                }
            }
            approvalMapper.updateRequestStatus(approvalId, "APPROVED", LocalDateTime.now());
            return;
        }

        // ── CARD_PUBLISH / CARD_UPDATE 승인 ─────────────────────
        CardVersion cardVersion = approvalMapper.findVersionByApprovalId(approvalId);
        if (cardVersion == null) {
            approvalMapper.updateRequestStatus(approvalId, "APPROVED", LocalDateTime.now());
            log.info("[결재승인] 비카드 결재 완료: approvalId={}", approvalId);
            return;
        }

        Long   cardId  = cardVersion.getCardId();
        String snapshot = cardVersion.getSnapshotJson();

        try {
            // objectMapper.readValue → model.Card 직접 역직렬화
            // Card에 선언된 @JsonAlias("updateBy")가 구 스냅샷 JSON 호환 보장
        	CardSnapshot snap = objectMapper.readValue(snapshot, CardSnapshot.class);
        	Card snapshotCard = snap.getCard();

            String previousStatus = Optional.ofNullable(cardMapper.findById(cardId))
                    .map(Card::getCardStatus)
                    .orElse("DRAFT");

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
                    .searchableYn(snapshotCard.getSearchableYn() != null ? snapshotCard.getSearchableYn() : "Y")
                    .visibleYn(snapshotCard.getVisibleYn()       != null ? snapshotCard.getVisibleYn()    : "Y")
                    .approvalRequiredYn(snapshotCard.getApprovalRequiredYn() != null
                            ? snapshotCard.getApprovalRequiredYn() : "Y")
                    .cardStatus("APPROVED")   // 결재 승인 → APPROVED, 스케줄러가 PUBLISHED 전환
                    .publishStartAt(snapshotCard.getPublishStartAt())
                    .publishEndAt(snapshotCard.getPublishEndAt())
                    .updatedBy(adminId)
                    .build();

            cardMapper.updateCard(approvedCard);          
            cardMapper.updateCardStatus(cardId, "APPROVED");

            cardVersionMapper.updateVersionStatus(       
                    cardVersion.getVersionId(), "APPROVED", adminId);

            cardStatusHistoryMapper.insertCardStatusHistory(
                    CardStatusHistory.builder()
                            .cardId(cardId)
                            .previousStatus(previousStatus)
                            .changedStatus("APPROVED")
                            .changedBy(adminId)
                            .changedReason("결재 승인 완료: approvalId=" + approvalId)
                            .build()
            );

            log.info("[결재승인] 전체 완료 → CARDS APPROVED: approvalId={}, cardId={}, versionId={}",
                    approvalId, cardId, cardVersion.getVersionId());

        } catch (JsonProcessingException e) {
            log.error("[결재승인] snapshot 역직렬화 실패: approvalId={}, versionId={}",
                    approvalId, cardVersion.getVersionId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카드 데이터 복원 실패. 관리자에게 문의하세요.");
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
                .map(ApprovalLine::getApprovalLineId)
                .findFirst()
                .orElse(null);

        if (lineId != null) {
            approvalMapper.updateLineStatus(lineId, "REJECTED",
                    request.getComment(), LocalDateTime.now());
        }

        // ── TERMS_PUBLISH 반려 ───────────────────────────────────
        if ("TERMS_PUBLISH".equals(approval.getRequestTypeCode())) {
            Long termsId = approval.getTargetId();
            if (termsId != null) {
                Terms terms = termsMapper.findById(termsId).orElse(null);
                if (terms != null) {
                    String previousStatus = terms.getStatus();
                    termsMapper.updateTermsStatus(termsId, "REVIEW", adminId);
                    termsMapper.insertStatusHistory(
                            termsId, previousStatus, "REVIEW", adminId,
                            "결재 반려: approvalId=" + approvalId);
                    log.info("[결재반려] 약관 상태 → REVIEW: approvalId={}, termsId={}", approvalId, termsId);
                }
            }
            approvalMapper.updateRequestStatus(approvalId, "REJECTED", LocalDateTime.now());
            return;
        }

        // ── CARD 결재 반려 ────────────────────────────────────────
        if (approval.getTargetId() != null) {
            CardVersion cardVersion = approvalMapper.findVersionByApprovalId(approvalId);
            cardVersionMapper.updateVersionStatus(         // ← cardVersionMapper2 → cardVersionMapper
                    approval.getTargetId(), "ARCHIVED", adminId);

            if (cardVersion != null && cardVersion.getCardId() != null) {
                String previousStatus = Optional.ofNullable(
                        cardMapper.findById(cardVersion.getCardId()))
                        .map(Card::getCardStatus)
                        .orElse("REVIEW");

                cardMapper.updateCardStatus(cardVersion.getCardId(), "REVIEW"); // ← cardMapper2 → cardMapper

                cardStatusHistoryMapper.insertCardStatusHistory(
                        CardStatusHistory.builder()
                                .cardId(cardVersion.getCardId())
                                .previousStatus(previousStatus)
                                .changedStatus("REVIEW")
                                .changedBy(adminId)
                                .changedReason("결재 반려: approvalId=" + approvalId)
                                .build()
                );

                log.info("[결재반려] 카드 상태 → REVIEW: approvalId={}, cardId={}, versionId={}",
                        approvalId, cardVersion.getCardId(), approval.getTargetId());
            }
        }

        approvalMapper.updateRequestStatus(approvalId, "REJECTED", LocalDateTime.now());
    }

    // buildCard2FromSnapshot() 완전 삭제 — model.Card 직접 사용으로 불필요
}
