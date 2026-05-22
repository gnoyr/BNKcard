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
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardContentMapper;
import com.bnk.domain.card.mapper.CardImageMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.mapper.CardVersionMapper2;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.card.model2.CardContent;
import com.bnk.domain.card.model2.CardVersion;
import com.bnk.domain.search.mapper.SearchLogMapper;
import com.bnk.domain.spending.mapper.SpendingPatternMapper;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.model.Terms;
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
    private final CardImageMapper cardImageMapper;
    private final CardContentMapper cardContentMapper;
    private final SpendingPatternMapper spendingPatternMapper;
    private final SearchLogMapper searchLogMapper;
    private final TermsMapper termsMapper;
    private final ApprovalMapper approvalMapper;
    private final ObjectMapper objectMapper;
    private final CardVersionMapper2 cardVersionMapper2;
    

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
        CardVersion version = CardVersion.builder()
                .cardId(card.getCardId())
                .versionNo("v1.0")
                .versionStatus("REVIEW")          // 상신과 동시에 REVIEW
                .snapshotJson(snapshotJson)
                .changeSummary(request.getChangeSummary())
                .createdBy(adminId)
                .build();
        cardVersionMapper2.insertCardVersion(version);  // selectKey로 versionId 주입

        // 4. APPROVAL_REQUESTS INSERT — target_id = version_id
        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_PUBLISH")
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())  // ← version_id로 변경
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);
        
        
        Map<String, Long> result = new HashMap<>();
        result.put("cardId", card.getCardId());
        result.put("versionId", version.getVersionId());
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

        // 기존 PUBLISHED 카드는 건드리지 않음 — 수정 버전만 새로 생성

        // 수정 요청 데이터로 snapshot 생성 (요청 데이터 기준)
        Card updatedSnapshot = Card.builder()
                .cardId(cardId)
                .cardCode(existing.getCardCode())           // 카드코드는 수정 불가 — existing 고정
                .cardType(request.getCardType() != null
                        ? request.getCardType() : existing.getCardType())
                .cardName(request.getCardName() != null
                        ? request.getCardName() : existing.getCardName())
                .companyName(request.getCompanyName() != null
                        ? request.getCompanyName() : existing.getCompanyName())
                .brandName(request.getBrandName() != null
                        ? request.getBrandName() : existing.getBrandName())
                .annualFeeDomestic(request.getAnnualFeeDomestic() != null
                        ? request.getAnnualFeeDomestic() : existing.getAnnualFeeDomestic())
                .annualFeeOverseas(request.getAnnualFeeOverseas() != null
                        ? request.getAnnualFeeOverseas() : existing.getAnnualFeeOverseas())
                .summaryDescription(request.getSummaryDescription() != null
                        ? request.getSummaryDescription() : existing.getSummaryDescription())
                .publishStartAt(request.getPublishStartAt() != null
                        ? request.getPublishStartAt() : existing.getPublishStartAt())
                .publishEndAt(request.getPublishEndAt() != null
                        ? request.getPublishEndAt() : existing.getPublishEndAt())
                // ↓ request에 없는 필드 — existing에서 그대로 복사
                .previousMonthSpend(existing.getPreviousMonthSpend())
                .minimumAge(existing.getMinimumAge())
                .maximumAge(existing.getMaximumAge())
                .targetUser(existing.getTargetUser())
                .searchableYn(existing.getSearchableYn())
                .visibleYn(existing.getVisibleYn())
                .approvalRequiredYn(existing.getApprovalRequiredYn())
                .createdBy(existing.getCreatedBy())
                .build();

        String snapshotJson = toSnapshotJson(updatedSnapshot);

        CardVersion version = CardVersion.builder()
                .cardId(cardId)
                .versionNo("v" + System.currentTimeMillis()) // 임시 — 추후 MAX+1로 개선
                .versionStatus("REVIEW")
                .snapshotJson(snapshotJson)
                .changeSummary(request.getChangeSummary())
                .createdBy(adminId)
                .build();
        cardVersionMapper2.insertCardVersion(version);

        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_UPDATE")
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())  // ← version_id
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);

        Map<String, Long> result = new HashMap<>();
        result.put("cardId", cardId);
        result.put("versionId", version.getVersionId());
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
                        .cardStatus(card.getCardStatus())
                        .publishStartAt(card.getPublishStartAt())  // 추가
                        .publishEndAt(card.getPublishEndAt()) 
                        .build())
                .collect(Collectors.toList());

        return PageResponse.of(content, totalCount, request.getPage(), request.getSize());
    }
    
    // 관리자 카드 상세 보기
    @Transactional(readOnly = true)
    public CardDetailResponse getAdminCardDetail(Long cardId) {
    	Card card = cardMapper.findById(cardId);
        if (card == null) {
            throw new IllegalArgumentException("존재하지 않는 카드입니다. cardId=" + cardId);
        }

        // 혜택 목록
        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(cardId);

        // 이미지 목록 (FRONT, BACK, THUMBNAIL, DETAIL 전부)
        List<CardImage> images = cardImageMapper.findByCardId(cardId);
        List<CardDetailResponse.ImageDto> imageDtos = images.stream()
                .map(img -> CardDetailResponse.ImageDto.builder()
                        .imageType(img.getImageType())
                        .imageUrl(img.getImageUrl())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        // 카드 콘텐츠 (INTRO/GUIDE/NOTICE 등) - display_order ASC
        List<CardContent> contents = cardContentMapper.findByCardId(cardId);
        List<CardDetailResponse.ContentDto> contentDtos = contents.stream()
                .map(c -> CardDetailResponse.ContentDto.builder()
                        .contentType(c.getContentType())
                        .title(c.getTitle())
                        .contentHtml(c.getContentHtml())
                        .mobileContentHtml(c.getMobileContentHtml())
                        .displayOrder(c.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        // 카드 신청 약관 (packageType = "CARD_APPLICATION" 고정)
        List<CardDetailResponse.TermsFileDto> termsFileDtos =
                termsMapper.findTermsFilesByCardId(cardId);        
        
        /*
        List<Terms> termsList = termsMapper.findByPackageType("CARD_APPLICATION");
        List<CardDetailResponse.TermsFileDto> termsFileDtos = termsList.stream()
                .map(t -> CardDetailResponse.TermsFileDto.builder()
                        .termsId(t.getTermsId())
                        .title(t.getTitle())
                        // Terms 모델에 filePath/fileType이 없으므로 null 처리
                        // 실제 파일 경로가 필요하면 TERMS_MASTERS에 컬럼 추가 필요
                        .filePath(null)
                        .fileType(null)
                        .build())
                .collect(Collectors.toList());
         */
        return CardDetailResponse.builder()
                .cardId(card.getCardId())
                .cardName(card.getCardName())
                .companyName(card.getCompanyName())
                .cardType(card.getCardType())
                .cardStatus(card.getCardStatus())         // 추가
                .annualFeeDomestic(card.getAnnualFeeDomestic())
                .annualFeeOverseas(card.getAnnualFeeOverseas())
                .summaryDescription(card.getSummaryDescription())
                .publishStartAt(card.getPublishStartAt()) // 추가
                .publishEndAt(card.getPublishEndAt())     // 추가
                .benefits(benefits)
                .images(imageDtos)
                .contents(contentDtos)
                .termsFiles(termsFileDtos)
                .build();
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