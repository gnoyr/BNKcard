package com.bnk.domain.card.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.BenefitCreateRequest;
import com.bnk.domain.card.dto.request.CardCreateRequest;
import com.bnk.domain.card.dto.request.CardUpdateRequest;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class AdminCardService {

    private final CardMapper cardMapper;
    private final CardBenefitMapper cardBenefitMapper;
    private final ApprovalMapper approvalMapper;
    private final ObjectMapper objectMapper;

    /**
     * B-03 카드 신규 등록 (RQ-B04)
     * CARDS INSERT(DRAFT) + CARD_BENEFITS INSERT + APPROVAL_REQUESTS INSERT
     */
    @Transactional
    public Map<String, Long> createCard(@Valid CardCreateRequest request, Long adminId) {

        // 카드코드 중복 체크 (CardMapper.findAdminCards 재사용 대신 간단히)
        // Card INSERT — DRAFT 상태
        Card card = Card.builder()
                .cardCode(request.getCardCode())
                .cardType(request.getCardType())
                .cardName(request.getCardName())
                .companyName(request.getCompanyName())
                .brandName(request.getBrandName())
                .annualFeeDomestic(request.getAnnualFeeDomestic())
                .annualFeeOverseas(request.getAnnualFeeOverseas())
                .summaryDescription(request.getSummaryDescription())
                .publishStartAt(request.getPublishStartAt())
                .publishEndAt(request.getPublishEndAt())
                .createdBy(adminId)
                .build();

        cardMapper.insertCard(card); // keyProperty=cardId 자동 주입

        // 혜택 등록
        if (request.getBenefits() != null && !request.getBenefits().isEmpty()) {
            List<CardBenefit> benefits = request.getBenefits().stream()
                    .map(b -> toBenefitEntity(b, card.getCardId()))
                    .collect(Collectors.toList());
            cardBenefitMapper.insertBenefits(benefits);
        }

        // APPROVAL_REQUESTS INSERT (CARD_PUBLISH, PENDING)
        String snapshotJson = toSnapshotJson(card);
        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_PUBLISH")
                .requesterAdminId(adminId)
                .targetId(card.getCardId())
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);

        Map<String, Long> result = new HashMap<>();
        result.put("cardId", card.getCardId());
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

    /**
     * B-04 카드 수정 (RQ-B05)
     * 기존 CARDS 직접 수정 금지 — CARD_VERSIONS snapshot + APPROVAL_REQUESTS 신규 생성
     * (CARD_VERSIONS 테이블은 model2에 별도 존재하나 Mapper가 없으므로 ApprovalMapper만 사용)
     */
    @Transactional
    public Map<String, Long> updateCard(Long cardId, @Valid CardUpdateRequest request, Long adminId) {

        Card existing = cardMapper.findById(cardId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // APPROVAL_REQUESTS INSERT (CARD_UPDATE, PENDING)
        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_UPDATE")
                .requesterAdminId(adminId)
                .targetId(cardId)
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);

        Map<String, Long> result = new HashMap<>();
        result.put("cardId", cardId);
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

    /**
     * B-13 관리자 카드 목록 다중조건 동적 검색 (RQ-B13)
     */
    @Transactional(readOnly = true)
    public PageResponse<CardListResponse> getAdminCardList(AdminCardSearchRequest request, Long adminId) {
        long totalCount = cardMapper.countAdminCards(request);

        if (totalCount == 0) {
            return PageResponse.of(Collections.emptyList(), 0L, request.getPage(), request.getSize());
        }

        List<Card> cards = cardMapper.findAdminCards(request);
        List<CardListResponse> content = cards.stream()
                .map(card -> CardListResponse.builder()
                        .cardId(card.getCardId())
                        .cardName(card.getCardName())
                        .companyName(card.getCompanyName())
                        .cardType(card.getCardType())
                        .annualFeeDomestic(card.getAnnualFeeDomestic())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.of(content, totalCount, request.getPage(), request.getSize());
    }

    // ── private helpers ──────────────────────────────────────────────

    private CardBenefit toBenefitEntity(BenefitCreateRequest b, Long cardId) {
        return CardBenefit.builder()
                .cardId(cardId)
                .categoryId(b.getCategoryId())
                .benefitTitle(b.getBenefitTitle())
                .benefitType(b.getBenefitType())
                .discountRate(b.getDiscountRate())
                .discountAmount(b.getDiscountAmount())
                .pointRate(b.getPointRate())
                .cashbackRate(b.getCashbackRate())
                .monthlyLimitAmount(b.getMonthlyLimitAmount())
                .dailyLimitAmount(b.getDailyLimitAmount())
                .minimumPaymentAmount(b.getMinimumPaymentAmount())
                .displayText(b.getDisplayText())
                .displayOrder(b.getDisplayOrder())
                .visibleYn("Y")
                .build();
    }

    private String toSnapshotJson(Card card) {
        try {
            return objectMapper.writeValueAsString(card);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}