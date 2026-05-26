package com.bnk.domain.card.service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalLine;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.BenefitCreateRequest;
import com.bnk.domain.card.dto.request.BenefitUpdateRequest;
import com.bnk.domain.card.dto.request.CardCreateRequest;
import com.bnk.domain.card.dto.request.CardSnapshot;
import com.bnk.domain.card.dto.request.CardStatusRequest;
import com.bnk.domain.card.dto.request.CardUpdateRequest;
import com.bnk.domain.card.dto.request.ContentUpdateRequest;
import com.bnk.domain.card.dto.request.ImageCreateRequest;
import com.bnk.domain.card.dto.request.ImageUpdateRequest;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardContentMapper;
import com.bnk.domain.card.mapper.CardImageMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.mapper.CardMapper2;
import com.bnk.domain.card.mapper.CardStatusHistoryMapper;
import com.bnk.domain.card.mapper.CardVersionMapper2;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardContent;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.card.model.CardStatusHistory;
import com.bnk.domain.card.model2.CardVersion;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class AdminCardService {

    private final CardMapper cardMapper;
    private final CardMapper2 cardMapper2;
    private final CardBenefitMapper cardBenefitMapper;
    private final CardImageMapper cardImageMapper;
    private final CardContentMapper cardContentMapper;
    private final TermsMapper termsMapper;
    private final ApprovalMapper approvalMapper;
    private final ObjectMapper objectMapper;
    private final CardVersionMapper2 cardVersionMapper2;
    private final CardStatusHistoryMapper cardStatusHistoryMapper;

    /**
     * B-03 카드 신규 등록 (RQ-B04)
     * CARDS INSERT(DRAFT) + CARD_BENEFITS INSERT + APPROVAL_REQUESTS INSERT
     */
    @Transactional
    public Map<String, Long> createCard(@Valid CardCreateRequest request, Long adminId) {

        // 카드코드 중복 체크 (CardMapper.findAdminCards 재사용 대신 간단히)
    	// 1. CARDS INSERT (DRAFT)
    	Card card = Card.builder()
    	        .cardCode(request.getCardCode())
    	        .cardType(request.getCardType())
    	        .cardName(request.getCardName())
    	        .companyName(request.getCompanyName())
    	        .brandName(request.getBrandName())
    	        .annualFeeDomestic(request.getAnnualFeeDomestic())
    	        .annualFeeOverseas(request.getAnnualFeeOverseas())
    	        .previousMonthSpend(request.getPreviousMonthSpend() != null    // ← 추가
    	                ? request.getPreviousMonthSpend() : 0L)
    	        .minimumAge(request.getMinimumAge())                           // ← 추가
    	        .maximumAge(request.getMaximumAge())                           // ← 추가
    	        .targetUser(request.getTargetUser())                           // ← 추가
    	        .summaryDescription(request.getSummaryDescription())
    	        .publishStartAt(request.getPublishStartAt())
    	        .publishEndAt(request.getPublishEndAt())
    	        .createdBy(adminId)
    	        .build();

        cardMapper.insertCard(card); // keyProperty=cardId 자동 주입

        // 2. CARD_BENEFITS INSERT
        List<CardBenefit> benefits = new ArrayList<>();
        if (request.getBenefits() != null && !request.getBenefits().isEmpty()) {
            benefits = request.getBenefits().stream()
                    .map(b -> toBenefitEntity(b, card.getCardId()))
                    .collect(Collectors.toList());
            cardBenefitMapper.insertBenefits(benefits);
        }

        // 3. CARD_IMAGES INSERT
        List<CardImage> images = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            images = request.getImages().stream()
                    .map(img -> toImageEntity(img, card.getCardId()))  // ← 이렇게
                    .collect(Collectors.toList());
            cardImageMapper.insertImages(images);
        }
        
        // 4. CARD_STATUS_HISTORIES INSERT (DRAFT 최초 이력)
        cardStatusHistoryMapper.insertCardStatusHistory(CardStatusHistory.builder()
                .cardId(card.getCardId())
                .previousStatus(null)
                .changedStatus("DRAFT")
                .changedBy(adminId)
                .changedReason("카드 신규 등록")
                .build());
        
        // 5. 스냅샷 구성 (기본정보 + 혜택 + 이미지 전부 포함)
        CardSnapshot snapshot = CardSnapshot.builder()
                .card(card)
                .benefits(benefits)
                .images(images)       // ← imageUrls 대신 CardImage 리스트
                .build();
        
        // 6. CARD_VERSIONS INSERT
        CardVersion version = CardVersion.builder()
                .cardId(card.getCardId())
                .versionNo("v1.0")
                .versionStatus("REVIEW")
                .snapshotJson(toSnapshotJson(snapshot))  // ← snapshot 객체로 변경
                .changeSummary(request.getChangeSummary())
                .createdBy(adminId)
                .build();
        cardVersionMapper2.insertCardVersion(version);

        // 7. APPROVAL_REQUESTS INSERT
        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_PUBLISH")
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);
        
        // 8. APPROVAL_LINES INSERT
        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(adminId)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);
        
        
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
                .cardCode(existing.getCardCode())
                .cardType(existing.getCardType())               // 수정 불가 — existing 고정
                .companyName(existing.getCompanyName())         // 수정 불가
                .companyCode(existing.getCompanyCode())         // 수정 불가
                .brandName(existing.getBrandName())             // 수정 불가
                .cardName(existing.getCardName())				// 수정 불가
                .annualFeeDomestic(request.getAnnualFeeDomestic() != null
                        ? request.getAnnualFeeDomestic() : existing.getAnnualFeeDomestic())
                .annualFeeOverseas(request.getAnnualFeeOverseas() != null
                        ? request.getAnnualFeeOverseas() : existing.getAnnualFeeOverseas())
                .previousMonthSpend(request.getPreviousMonthSpend() != null
                        ? request.getPreviousMonthSpend() : existing.getPreviousMonthSpend())
                .minimumAge(request.getMinimumAge() != null
                        ? request.getMinimumAge() : existing.getMinimumAge())
                .maximumAge(request.getMaximumAge() != null
                        ? request.getMaximumAge() : existing.getMaximumAge())
                .creditLimitMin(request.getCreditLimitMin() != null
                        ? request.getCreditLimitMin() : existing.getCreditLimitMin())
                .creditLimitMax(request.getCreditLimitMax() != null
                        ? request.getCreditLimitMax() : existing.getCreditLimitMax())
                .targetUser(request.getTargetUser() != null
                        ? request.getTargetUser() : existing.getTargetUser())
                .summaryDescription(request.getSummaryDescription() != null
                        ? request.getSummaryDescription() : existing.getSummaryDescription())
                .publishStartAt(request.getPublishStartAt() != null
                        ? request.getPublishStartAt() : existing.getPublishStartAt())
                .publishEndAt(request.getPublishEndAt() != null
                        ? request.getPublishEndAt() : existing.getPublishEndAt())
                .searchableYn(request.getSearchableYn() != null
                        ? request.getSearchableYn() : existing.getSearchableYn())
                .visibleYn(request.getVisibleYn() != null
                        ? request.getVisibleYn() : existing.getVisibleYn())
                // existing 고정
                .approvalRequiredYn(existing.getApprovalRequiredYn())
                .cardStatus(existing.getCardStatus())
                .applicationCount(existing.getApplicationCount())
                .createdBy(existing.getCreatedBy())
                .createdAt(existing.getCreatedAt())
                .updatedBy(adminId)                    // ← 추가
                .updatedAt(LocalDateTime.now())        // ← 추가
                .deletedYn(existing.getDeletedYn())
                .deletedAt(existing.getDeletedAt())
                .build();
        
        // 혜택, 이미지 현재 DB에서 조회 및 스냅샷 생성
        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(cardId);
        List<CardImage> images = cardImageMapper.findByCardId(cardId);

        CardSnapshot snapshot = CardSnapshot.builder()
                .card(updatedSnapshot)
                .benefits(benefits)
                .images(images)
                .build();        
        String snapshotJson = toSnapshotJson(snapshot);

        
        // 카드 버전 등록
        int nextNo = cardVersionMapper2.getLatestVersionSeq(cardId) + 1;
        CardVersion version = CardVersion.builder()
                .cardId(cardId)
                .versionNo("v" + nextNo + ".0")   // v1.0, v2.0, v3.0 ...
                .versionStatus("REVIEW")
                .snapshotJson(snapshotJson)
                .changeSummary(request.getChangeSummary())
                .createdBy(adminId)
                .build();
        cardVersionMapper2.insertCardVersion(version);

        // 결재 신청서 등록
        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_UPDATE")
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())  // ← version_id
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);
        
        // 결재 라인 등록
        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(adminId)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);

        Map<String, Long> result = new HashMap<>();
        result.put("cardId", cardId);
        result.put("versionId", version.getVersionId());
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

    // ── 공통 — 스냅샷 + 결재신청 생성 (혜택/이미지 수정용) ──
    protected Map<String, Long> createVersionAndApproval(Long cardId, Long adminId,
                                                        String changeSummary,
                                                        String requestTypeCode) {
        // 현재 DB 상태 전체 조회
        Card card = cardMapper.findById(cardId);
        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(cardId);
        List<CardImage> images = cardImageMapper.findByCardId(cardId);

        // 버전 번호
        int nextNo = cardVersionMapper2.getLatestVersionSeq(cardId) + 1;

        CardSnapshot snapshot = CardSnapshot.builder()
                .card(card)
                .benefits(benefits)
                .images(images)
                .build();

        CardVersion version = CardVersion.builder()
                .cardId(cardId)
                .versionNo("v" + nextNo + ".0")
                .versionStatus("REVIEW")
                .snapshotJson(toSnapshotJson(snapshot))
                .changeSummary(changeSummary)
                .createdBy(adminId)
                .build();
        cardVersionMapper2.insertCardVersion(version);

        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode(requestTypeCode)
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())
                .requestComment(changeSummary)
                .build();
        approvalMapper.insertApprovalRequest(approval);

        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(adminId)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);

        return Map.of(
                "versionId", version.getVersionId(),
                "approvalId", approval.getApprovalId()
        );
    }
    
    // ── 혜택 등록/수정 ──────────────────────────────────────
    @Transactional
    public Map<String, Long> saveCardBenefits(Long cardId, BenefitUpdateRequest request, Long adminId) {
        cardBenefitMapper.deleteByCardId(cardId);

        if (request.getBenefits() != null && !request.getBenefits().isEmpty()) {
            List<CardBenefit> benefits = request.getBenefits().stream()
                    .map(b -> toBenefitEntity(b, cardId))
                    .collect(Collectors.toList());
            cardBenefitMapper.insertBenefits(benefits);
        }

        Map<String, Long> result = new HashMap<>(
                createVersionAndApproval(cardId, adminId, request.getChangeSummary(), "CARD_UPDATE"));
        result.put("cardId", cardId);
        return result;
    }
    
    // ── 이미지 등록/수정 ─────────────────────────────────────
    @Transactional
    public Map<String, Long> saveCardImages(Long cardId, ImageUpdateRequest request, Long adminId) {
        cardImageMapper.deleteByCardId(cardId);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<CardImage> images = request.getImages().stream()
                    .map(img -> toImageEntity(img, cardId))
                    .collect(Collectors.toList());
            cardImageMapper.insertImages(images);
        }

        Map<String, Long> result = new HashMap<>(
                createVersionAndApproval(cardId, adminId, request.getChangeSummary(), "CARD_UPDATE"));
        result.put("cardId", cardId);
        return result;
    }
    
    // ── 콘텐츠 등록/수정 ─────────────────────────────────────
    @Transactional
    public void saveCardContents(Long cardId, ContentUpdateRequest request, Long adminId) {
        cardContentMapper.deleteByCardId(cardId);

        if (request.getContents() != null && !request.getContents().isEmpty()) {
            List<CardContent> contents = request.getContents().stream()
                    .map(c -> CardContent.builder()
                            .cardId(cardId)
                            .contentType(c.getContentType())
                            .title(c.getTitle())
                            .contentHtml(c.getContentHtml())
                            .mobileContentHtml(c.getMobileContentHtml())
                            .displayOrder(c.getDisplayOrder())
                            .visibleYn(c.getVisibleYn() != null ? c.getVisibleYn() : "Y")
                            .createdBy(adminId)
                            .build())
                    .collect(Collectors.toList());
            cardContentMapper.insertContents(contents);
        }
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
    


    /**
     * 카드 상태 강제 변경 (B-관리자 수동 처리 / 긴급 중지).
     * 스케줄러와 별도로 관리자가 직접 상태를 변경할 때 사용.
     */
    @Transactional
    public void changeCardStatus(Long cardId, CardStatusRequest request, Long adminId) {

        com.bnk.domain.card.model2.Card card = cardMapper2.getCardDetail(cardId);
        if (card == null) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        String previousStatus = card.getCardStatus();
        String newStatus      = request.getCardStatus();

        if (previousStatus.equals(newStatus)) {
            return;
        }

        cardMapper2.updateCardStatus(cardId, newStatus);

        cardStatusHistoryMapper.insertCardStatusHistory(
                CardStatusHistory.builder()
                        .cardId(cardId)
                        .previousStatus(previousStatus)
                        .changedStatus(newStatus)
                        .changedBy(adminId)
                        .changedReason(request.getChangedReason() != null
                                ? request.getChangedReason()
                                : "관리자 수동 상태 변경")
                        .build()
        );

        log.info("[카드상태변경] cardId={}, {} → {}, adminId={}",
                cardId, previousStatus, newStatus, adminId);
        }

    // 관리자 카드 상세 보기
    @Transactional(readOnly = true)
    public CardDetailResponse getAdminCardDetail(Long cardId) {
    	com.bnk.domain.card.model2.Card card = cardMapper2.getCardDetail(cardId);
        if (card == null) {
            throw new IllegalArgumentException("존재하지 않는 카드입니다. cardId=" + cardId);
        }

        // 혜택 목록
        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(cardId);

        // 이미지 목록 (FRONT, BACK, THUMBNAIL, DETAIL 전부)
        List<CardImage> images = cardImageMapper.findByCardId(cardId);
        List<CardDetailResponse.ImageDto> imageDtos = images.stream()
        	    .map(img -> CardDetailResponse.ImageDto.builder()
        	        .imageId(img.getImageId())
        	        .imageType(img.getImageType())
        	        .imageUrl(img.getImageUrl())
        	        .originalName(img.getOriginalName())
        	        .storedName(img.getStoredName())
        	        .fileSize(img.getFileSize())
        	        .mimeType(img.getMimeType())
        	        .imageWidth(img.getImageWidth())
        	        .imageHeight(img.getImageHeight())
        	        .sortOrder(img.getSortOrder())
        	        .createdAt(img.getCreatedAt())
        	        .build())
        	    .collect(Collectors.toList());

        // 카드 콘텐츠 (INTRO/GUIDE/NOTICE 등) - display_order ASC
        List<CardContent> contents = cardContentMapper.findByCardId(cardId);
        List<CardDetailResponse.ContentDto> contentDtos = contents.stream()
        	    .map(c -> CardDetailResponse.ContentDto.builder()
        	        .contentId(c.getContentId())        // ← 추가
        	        .cardId(c.getCardId())              // ← 추가
        	        .contentType(c.getContentType())
        	        .title(c.getTitle())
        	        .contentHtml(c.getContentHtml())
        	        .mobileContentHtml(c.getMobileContentHtml())
        	        .displayOrder(c.getDisplayOrder())
        	        .visibleYn(c.getVisibleYn())        // ← 추가
        	        .createdBy(c.getCreatedBy())        // ← 추가
        	        .createdAt(c.getCreatedAt())        // ← 추가
        	        .updatedAt(c.getUpdatedAt())        // ← 추가
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
                .cardCode(card.getCardCode())
                .cardType(card.getCardType())
                .cardName(card.getCardName())
                .companyName(card.getCompanyName())
                .companyCode(card.getCompanyCode())
                .brandName(card.getBrandName())
                .annualFeeDomestic(card.getAnnualFeeDomestic())
                .annualFeeOverseas(card.getAnnualFeeOverseas())
                .previousMonthSpend(card.getPreviousMonthSpend())
                .minimumAge(card.getMinimumAge())
                .maximumAge(card.getMaximumAge())
                .creditLimitMin(card.getCreditLimitMin())
                .creditLimitMax(card.getCreditLimitMax())
                .targetUser(card.getTargetUser())
                .summaryDescription(card.getSummaryDescription())
                .searchableYn(card.getSearchableYn())
                .visibleYn(card.getVisibleYn())
                .approvalRequiredYn(card.getApprovalRequiredYn())
                .cardStatus(card.getCardStatus())
                .publishStartAt(card.getPublishStartAt())
                .publishEndAt(card.getPublishEndAt())
                .applicationCount(card.getApplicationCount())
                .createdBy(card.getCreatedBy())
                .createdAt(card.getCreatedAt())
                .updateBy(card.getUpdateBy())
                .updateAt(card.getUpdateAt())
                .deletedYn(card.getDeletedYn())
                .deleteAt(card.getDeleteAt())
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
    
    private CardImage toImageEntity(ImageCreateRequest img, Long cardId) {
        return CardImage.builder()
                .cardId(cardId)
                .imageType(img.getImageType())
                .imageUrl(img.getImageUrl())
                .originalName(img.getOriginalName())
                .storedName(img.getStoredName())
                .fileSize(img.getFileSize())
                .mimeType(img.getMimeType())
                .imageWidth(img.getImageWidth())
                .imageHeight(img.getImageHeight())
                .sortOrder(img.getSortOrder())
                .build();
    }

    private String toSnapshotJson(CardSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    
    
}