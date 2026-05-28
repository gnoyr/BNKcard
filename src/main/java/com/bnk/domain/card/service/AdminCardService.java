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
import com.bnk.domain.card.mapper.CardStatusHistoryMapper;
import com.bnk.domain.card.mapper.CardVersionMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardContent;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.card.model.CardStatusHistory;
import com.bnk.domain.card.model.CardVersion;
import com.bnk.domain.terms.mapper.TermsMapper;
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
public class AdminCardService {


    private final CardMapper             cardMapper;
    private final CardVersionMapper      cardVersionMapper;
    private final CardBenefitMapper      cardBenefitMapper;
    private final CardImageMapper        cardImageMapper;
    private final CardContentMapper      cardContentMapper;
    private final TermsMapper            termsMapper;
    private final ApprovalMapper         approvalMapper;
    private final ObjectMapper           objectMapper;
    private final CardStatusHistoryMapper cardStatusHistoryMapper;

    // ══════════════════════════════════════════════════════════════════
    // B-03 카드 신규 등록
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public Map<String, Long> createCard(@Valid CardCreateRequest request, Long adminId) {

        // 1. CARDS INSERT (DRAFT)
        Card card = Card.builder()
                .cardCode(request.getCardCode())
                .cardType(request.getCardType())
                .cardName(request.getCardName())
                .companyName(request.getCompanyName())
                .companyCode(request.getCompanyCode() != null ? request.getCompanyCode() : "01")
                .brandName(request.getBrandName())
                .annualFeeDomestic(request.getAnnualFeeDomestic())
                .annualFeeOverseas(request.getAnnualFeeOverseas())
                .previousMonthSpend(request.getPreviousMonthSpend() != null
                        ? request.getPreviousMonthSpend() : 0L)
                .minimumAge(request.getMinimumAge())
                .maximumAge(request.getMaximumAge())
                .creditLimitMin(request.getCreditLimitMin())
                .creditLimitMax(request.getCreditLimitMax())
                .targetUser(request.getTargetUser())
                .summaryDescription(request.getSummaryDescription())
                .searchableYn(request.getSearchableYn() != null ? request.getSearchableYn() : "Y")
                .visibleYn(request.getVisibleYn() != null ? request.getVisibleYn() : "Y")
                .publishStartAt(request.getPublishStartAt())
                .publishEndAt(request.getPublishEndAt())
                .createdBy(adminId)
                .build();

        cardMapper.insertCard(card);

        // 2. CARD_BENEFITS INSERT
        if (request.getBenefits() != null && !request.getBenefits().isEmpty()) {
        	List<CardBenefit> benefits = request.getBenefits().stream()
                    .map(b -> toBenefitEntity(b, card.getCardId()))
                    .collect(Collectors.toList());
            cardBenefitMapper.insertBenefits(benefits);
        }

        // 3. CARD_IMAGES INSERT
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<CardImage> images = request.getImages().stream()
                    .map(i -> toImageEntity(i, card.getCardId()))
                    .collect(Collectors.toList());
            cardImageMapper.insertImages(images);
        }

        // 4. 스냅샷
        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(card.getCardId());
        List<CardImage> images = cardImageMapper.findByCardId(card.getCardId());
        CardSnapshot snapshot = CardSnapshot.builder()
                .card(card)
                .benefits(benefits)
                .images(images)
                .build();
        String snapshotJson = toSnapshotJson(snapshot);

        // 5. CARD_VERSION INSERT
        CardVersion version = CardVersion.builder()
                .cardId(card.getCardId())
                .versionNo("v1.0")
                .versionStatus("REVIEW")
                .snapshotJson(snapshotJson)
                .changeSummary(request.getChangeSummary())
                .createdBy(adminId)
                .build();
        cardVersionMapper.insertCardVersion(version);

        // 6. APPROVAL_REQUEST INSERT
        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_PUBLISH")
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);

        // 7. APPROVAL_LINE INSERT
        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(1L)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);

        cardStatusHistoryMapper.insertCardStatusHistory(
                CardStatusHistory.builder()
                        .cardId(card.getCardId())
                        .previousStatus(null)
                        .changedStatus("DRAFT")
                        .changedBy(adminId)
                        .changedReason("카드 최초 등록")
                        .build()
        );

        Map<String, Long> result = new HashMap<>();
        result.put("cardId",     card.getCardId());
        result.put("versionId",  version.getVersionId());
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════
    // B-04 카드 기본정보 수정
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public Map<String, Long> updateCard(Long cardId, @Valid CardUpdateRequest request, Long adminId) {

        Card existing = cardMapper.findById(cardId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        Card updatedSnapshot = Card.builder()
                .cardId(cardId)
                .cardCode(existing.getCardCode())
                .cardType(request.getCardType()   != null ? request.getCardType()   : existing.getCardType())
                .cardName(request.getCardName()   != null ? request.getCardName()   : existing.getCardName())
                .companyName(request.getCompanyName() != null ? request.getCompanyName() : existing.getCompanyName())
                .companyCode(existing.getCompanyCode())
                .brandName(request.getBrandName() != null ? request.getBrandName() : existing.getBrandName())
                .annualFeeDomestic(request.getAnnualFeeDomestic() != null
                        ? request.getAnnualFeeDomestic() : existing.getAnnualFeeDomestic())
                .annualFeeOverseas(request.getAnnualFeeOverseas() != null
                        ? request.getAnnualFeeOverseas() : existing.getAnnualFeeOverseas())
                .previousMonthSpend(request.getPreviousMonthSpend() != null
                        ? request.getPreviousMonthSpend() : existing.getPreviousMonthSpend())
                .minimumAge(request.getMinimumAge() != null ? request.getMinimumAge() : existing.getMinimumAge())
                .maximumAge(request.getMaximumAge() != null ? request.getMaximumAge() : existing.getMaximumAge())
                .creditLimitMin(request.getCreditLimitMin() != null
                        ? request.getCreditLimitMin() : existing.getCreditLimitMin())
                .creditLimitMax(request.getCreditLimitMax() != null
                        ? request.getCreditLimitMax() : existing.getCreditLimitMax())
                .targetUser(request.getTargetUser() != null ? request.getTargetUser() : existing.getTargetUser())
                .summaryDescription(request.getSummaryDescription() != null
                        ? request.getSummaryDescription() : existing.getSummaryDescription())
                .searchableYn(request.getSearchableYn() != null ? request.getSearchableYn() : existing.getSearchableYn())
                .visibleYn(request.getVisibleYn()       != null ? request.getVisibleYn()     : existing.getVisibleYn())
                .deletedYn(request.getDeletedYn()       != null ? request.getDeletedYn()     : existing.getDeletedYn())
                .deletedAt(("Y".equals(request.getDeletedYn()) && !"Y".equals(existing.getDeletedYn()))
                        ? LocalDateTime.now() : existing.getDeletedAt())
                .cardStatus(request.getCardStatus() != null ? request.getCardStatus() : existing.getCardStatus())
                .approvalRequiredYn(existing.getApprovalRequiredYn())
                .applicationCount(existing.getApplicationCount())
                .createdBy(existing.getCreatedBy())
                .createdAt(existing.getCreatedAt())
                .updatedBy(adminId)
                .updatedAt(LocalDateTime.now())
                .build();

        // 상태 변경 이력
        String previousStatus = existing.getCardStatus();
        String newStatus      = request.getCardStatus();
        if (newStatus != null && !newStatus.equals(previousStatus)) {
            cardStatusHistoryMapper.insertCardStatusHistory(
                    CardStatusHistory.builder()
                            .cardId(cardId)
                            .previousStatus(previousStatus)
                            .changedStatus(newStatus)
                            .changedBy(adminId)
                            .changedReason(request.getChangeSummary())
                            .build()
            );
        }

        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(cardId);
        List<CardImage>   images   = cardImageMapper.findByCardId(cardId);

        CardSnapshot snapshot = CardSnapshot.builder()
                .card(updatedSnapshot)
                .benefits(benefits)
                .images(images)
                .build();
        String snapshotJson = toSnapshotJson(snapshot);

        int nextNo = cardVersionMapper.getLatestVersionSeq(cardId) + 1;  
        CardVersion version = CardVersion.builder()
                .cardId(cardId)
                .versionNo("v" + nextNo + ".0")
                .versionStatus("REVIEW")
                .snapshotJson(snapshotJson)
                .changeSummary(request.getChangeSummary())
                .createdBy(adminId)
                .build();
        cardVersionMapper.insertCardVersion(version);

        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("CARD_UPDATE")
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);

        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(1L)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);

        Map<String, Long> result = new HashMap<>();
        result.put("cardId",     cardId);
        result.put("versionId",  version.getVersionId());
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════
    // 혜택 등록/수정
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public Map<String, Long> saveCardBenefits(Long cardId, @Valid BenefitUpdateRequest request, Long adminId) {
        Card existing = cardMapper.findById(cardId);
        if (existing == null) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        cardBenefitMapper.deleteByCardId(cardId);

        if (request.getBenefits() != null && !request.getBenefits().isEmpty()) {
            List<CardBenefit> benefits = request.getBenefits().stream()
                    .map(b -> toBenefitEntity(b, cardId))
                    .collect(Collectors.toList());
            cardBenefitMapper.insertBenefits(benefits);
        }

        return createVersionAndApproval(cardId, adminId, request.getChangeSummary(), "CARD_UPDATE");
    }

    // ══════════════════════════════════════════════════════════════════
    // 이미지 등록/수정
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public Map<String, Long> saveCardImages(Long cardId, @Valid ImageUpdateRequest request, Long adminId) {
        Card existing = cardMapper.findById(cardId);
        if (existing == null) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        cardImageMapper.deleteByCardId(cardId);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<CardImage> images = request.getImages().stream()
                    .map(i -> toImageEntity(i, cardId))
                    .collect(Collectors.toList());
            cardImageMapper.insertImages(images);
        }

        return createVersionAndApproval(cardId, adminId, request.getChangeSummary(), "CARD_UPDATE");
    }

    // ══════════════════════════════════════════════════════════════════
    // 콘텐츠 등록/수정
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public void saveCardContents(Long cardId, @Valid ContentUpdateRequest request, Long adminId) {
        Card existing = cardMapper.findById(cardId);
        if (existing == null) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

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

    // ══════════════════════════════════════════════════════════════════
    // 카드 상태 강제 변경
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public void changeCardStatus(Long cardId, @Valid CardStatusRequest request, Long adminId) {
        // ← cardMapper2.getCardDetail() 대신 cardMapper.findById() 사용
        Card card = cardMapper.findById(cardId);
        if (card == null) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        String previousStatus = card.getCardStatus();
        String newStatus      = request.getCardStatus();

        if (previousStatus.equals(newStatus)) {
            return;
        }

        cardMapper.updateCardStatus(cardId, newStatus);   // ← cardMapper2 → cardMapper

        cardStatusHistoryMapper.insertCardStatusHistory(
                CardStatusHistory.builder()
                        .cardId(cardId)
                        .previousStatus(previousStatus)
                        .changedStatus(newStatus)
                        .changedBy(adminId)
                        .changedReason(request.getChangedReason() != null
                                ? request.getChangedReason() : "관리자 수동 변경")
                        .build()
        );
    }

    // ══════════════════════════════════════════════════════════════════
    // 관리자 카드 목록
    // ══════════════════════════════════════════════════════════════════
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
                        .publishStartAt(card.getPublishStartAt())
                        .publishEndAt(card.getPublishEndAt())
                        .build())
                .collect(Collectors.toList());
        return PageResponse.of(content, totalCount, request.getPage(), request.getSize());
    }

    // ══════════════════════════════════════════════════════════════════
    // 관리자 카드 상세
    // ══════════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public CardDetailResponse getAdminCardDetail(Long cardId) {
        Card card = cardMapper.findById(cardId);
        if (card == null) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        List<CardBenefit>  benefits = cardBenefitMapper.findByCardId(cardId);
        List<CardImage>    images   = cardImageMapper.findByCardId(cardId);
        List<CardContent>  contents = cardContentMapper.findByCardId(cardId);
        List<CardDetailResponse.TermsFileDto> termsFiles = termsMapper.findTermsFilesByCardId(cardId);

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
                .updateBy(card.getUpdatedBy())
                .updateAt(card.getUpdatedAt())
                .deletedYn(card.getDeletedYn())
                .deleteAt(card.getDeletedAt())
                // ── 연관 데이터 ────────────────────────────────────────
                .benefits(benefits)
                .images(images.stream()
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
                        .collect(Collectors.toList()))
                .contents(contents.stream()
                        .map(c -> CardDetailResponse.ContentDto.builder()
                                .contentId(c.getContentId())
                                .cardId(c.getCardId())
                                .contentType(c.getContentType())
                                .title(c.getTitle())
                                .contentHtml(c.getContentHtml())
                                .mobileContentHtml(c.getMobileContentHtml())
                                .displayOrder(c.getDisplayOrder())
                                .visibleYn(c.getVisibleYn())
                                .createdBy(c.getCreatedBy())
                                .createdAt(c.getCreatedAt())
                                .updatedAt(c.getUpdatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .termsFiles(termsFiles)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // Private Helpers
    // ══════════════════════════════════════════════════════════════════

    /** 혜택/이미지 수정 후 공통 버전+결재 생성 */
    protected Map<String, Long> createVersionAndApproval(Long cardId, Long adminId,
                                                         String changeSummary,
                                                         String requestTypeCode) {
        Card card             = cardMapper.findById(cardId);
        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(cardId);
        List<CardImage>   images   = cardImageMapper.findByCardId(cardId);

        int nextNo = cardVersionMapper.getLatestVersionSeq(cardId) + 1;  // ← CardVersionMapper

        CardSnapshot snapshot = CardSnapshot.builder()
                .card(card).benefits(benefits).images(images)
                .build();
        String snapshotJson = toSnapshotJson(snapshot);

        CardVersion version = CardVersion.builder()
                .cardId(cardId)
                .versionNo("v" + nextNo + ".0")
                .versionStatus("REVIEW")
                .snapshotJson(snapshotJson)
                .changeSummary(changeSummary)
                .createdBy(adminId)
                .build();
        cardVersionMapper.insertCardVersion(version);

        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode(requestTypeCode)
                .requesterAdminId(adminId)
                .targetId(version.getVersionId())
                .requestComment(changeSummary)
                .build();
        approvalMapper.insertApprovalRequest(approval);

        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(1L)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);

        Map<String, Long> result = new HashMap<>();
        result.put("cardId",     cardId);
        result.put("versionId",  version.getVersionId());
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

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
                .benefitCondition(b.getBenefitCondition())
                .displayText(b.getDisplayText())
                .displayOrder(b.getDisplayOrder())
                .visibleYn(b.getVisibleYn() != null ? b.getVisibleYn() : "Y")
                .build();
    }

    private CardImage toImageEntity(ImageCreateRequest i, Long cardId) {
        return CardImage.builder()
                .cardId(cardId)
                .imageType(i.getImageType())
                .imageUrl(i.getImageUrl())
                .originalName(i.getOriginalName())
                .storedName(i.getStoredName())
                .fileSize(i.getFileSize())
                .mimeType(i.getMimeType())
                .imageWidth(i.getImageWidth())
                .imageHeight(i.getImageHeight())
                .sortOrder(i.getSortOrder() != null ? i.getSortOrder() : 1)
                .build();
    }

    private String toSnapshotJson(CardSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("[스냅샷] 직렬화 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "스냅샷 생성 실패");
        }
    }
}
