package com.bnk.domain.card.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.card.dto.request.CardCompareRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.dto.request.CardSimulationRequest;
import com.bnk.domain.card.dto.response.BannerDto;
import com.bnk.domain.card.dto.response.BenefitDto;
import com.bnk.domain.card.dto.response.CardCompareResponse;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.dto.response.SimulationResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardContentMapper;   // 새로 만들어야 함
import com.bnk.domain.card.mapper.CardImageMapper;     // 새로 만들어야 함
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardContent;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.search.mapper.SearchLogMapper;
import com.bnk.domain.search.model.SearchLog;
import com.bnk.domain.spending.mapper.SpendingPatternMapper;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.global.log.annotation.Loggable;
import com.bnk.global.response.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardMapper cardMapper;
    private final CardBenefitMapper cardBenefitMapper;
    private final CardImageMapper cardImageMapper;
    private final CardContentMapper cardContentMapper;
    private final SpendingPatternMapper spendingPatternMapper;
    private final SearchLogMapper searchLogMapper;
    private final TermsMapper termsMapper;

    // ────────────────────────────────────────────────────────────────
    // 홈 배너 조회
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BannerDto> getHomeBanners(Long userId) {

        List<Card> cards;

        if (userId == null) {
            // ① 비로그인: 조회수 상위 3
            cards = cardMapper.findTop3ByViewCount();
        } else {
            // ② 우수회원: 최대 소비 카테고리 기반 TOP3
            Long topCategoryId = spendingPatternMapper.findTopCategoryIdByUserId(userId);
            cards = (topCategoryId != null)
                    ? cardMapper.findTop3ByCategoryId(topCategoryId)
                    : cardMapper.findTop3ByViewCount();
        }

        if (cards.isEmpty()) return Collections.emptyList();

        // 카드 ID 목록으로 FRONT 이미지 한 번에 조회 (N+1 방지)
        List<Long> cardIds = cards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        // FRONT 이미지 단건 조회 (배치 조회 대신 안전하게 단건 루프)
        Map<Long, String> imageUrlMap = new HashMap<>();
        for (Long cid : cardIds) {
            CardImage img = cardImageMapper.findByCardIdAndType(cid, "FRONT");
            if (img != null) imageUrlMap.put(cid, img.getImageUrl());
        }

        return cards.stream()
                .map(card -> BannerDto.builder()
                        .cardId(card.getCardId())
                        .cardName(card.getCardName())
                        .bannerImageUrl(imageUrlMap.get(card.getCardId()))
                        .companyName(card.getCompanyName())
                        .build())
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // 카드 목록 + 검색 (페이징)
    // ────────────────────────────────────────────────────────────────
    @Transactional
    @Loggable(eventType = "CARD_SEARCH", targetType = "CARD", 
    actionDetail = "카드검색", cardIdParam = "")
    public PageResponse<CardListResponse> getCardList(@Valid CardSearchRequest request, Long userId) {

        long totalCount = cardMapper.countAll(request);

        if (totalCount == 0) {
            // 검색어 로그는 결과 0건이어도 기록
            saveSearchLog(request.getQ(), userId, 0);
            return PageResponse.of(Collections.emptyList(), 0L, request.getPage(), request.getSize());
        }

        List<Card> cards = cardMapper.findAll(request);

        // 검색 로그 저장 (검색어 있을 때만)
        saveSearchLog(request.getQ(), userId, (int) totalCount);

        // 카드 ID 목록
        List<Long> cardIds = cards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        // 이미지 조회: THUMBNAIL 우선, 없으면 FRONT 폴백 (단건 루프)
        Map<Long, String> thumbnailMap = new HashMap<>();
        for (Long cid : cardIds) {
            log.info("=== 이미지 조회 시도 cardId: {}", cid);
            CardImage img = cardImageMapper.findByCardIdAndType(cid, "THUMBNAIL");
            if (img == null) img = cardImageMapper.findByCardIdAndType(cid, "FRONT");
            log.info("=== 조회 결과: {}", img != null ? img.getImageUrl() : "NULL");
            if (img != null) thumbnailMap.put(cid, img.getImageUrl());
        }

        // display_order = 1인 혜택 한 번에 조회 (새 Mapper 메서드 필요 - 아래 설명)
        List<CardBenefit> topBenefits = cardBenefitMapper.findTop1ByCardIds(cardIds);
        Map<Long, String> topBenefitMap = topBenefits.stream()
                .collect(Collectors.toMap(
                        CardBenefit::getCardId,
                        CardBenefit::getDisplayText,
                        (e, r) -> e
                ));

        List<CardListResponse> content = cards.stream()
        	    .map(card -> CardListResponse.builder()
        	        .cardId(card.getCardId())
        	        .cardName(card.getCardName())
        	        .companyName(card.getCompanyName())
        	        .cardType(card.getCardType())
        	        .annualFeeDomestic(card.getAnnualFeeDomestic())   // ← 추가
        	        .annualFeeOverseas(card.getAnnualFeeOverseas())   // ← 추가
        	        .thumbnailUrl(thumbnailMap.get(card.getCardId()))
        	        .topBenefit(topBenefitMap.get(card.getCardId()))
        	        .build())
        	    .collect(Collectors.toList());

        return PageResponse.of(content, totalCount, request.getPage(), request.getSize());
    }

    // ────────────────────────────────────────────────────────────────
    // TOP3 추천
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CardListResponse> getTop3Cards(Long userId, String surveyResult) {

        List<Card> cards;
        String recommendReason;

        if (userId == null && (surveyResult == null || surveyResult.isBlank())) {
            // ① 비회원
            cards = cardMapper.findTop3ByViewCount();
            recommendReason = "많은 분들이 찾는 카드";

        } else if (userId == null) {
            // ② 신규회원: surveyResult = categoryId 문자열
            Long categoryId = parseLong(surveyResult);
            cards = (categoryId != null)
                    ? cardMapper.findTop3ByCategoryId(categoryId)
                    : cardMapper.findTop3ByViewCount();
            recommendReason = "설문 결과 맞춤 추천";

        } else {
            // ③ 우수회원: 소비패턴 기반
            Long topCategoryId = spendingPatternMapper.findTopCategoryIdByUserId(userId);
            cards = (topCategoryId != null)
                    ? cardMapper.findTop3ByCategoryId(topCategoryId)
                    : cardMapper.findTop3ByViewCount();
            recommendReason = "소비패턴 기반 맞춤 추천";
        }

        if (cards.isEmpty()) return Collections.emptyList();

        // THUMBNAIL 이미지 한 번에 조회
        List<Long> cardIds = cards.stream().map(Card::getCardId).collect(Collectors.toList());
        
        // 이미지 조회: THUMBNAIL 우선, 없으면 FRONT 폴백 (단건 루프)
        Map<Long, String> thumbnailMap = new HashMap<>();
        for (Long cid : cardIds) {
            CardImage img = cardImageMapper.findByCardIdAndType(cid, "THUMBNAIL");
            if (img == null) img = cardImageMapper.findByCardIdAndType(cid, "FRONT");
            if (img != null) thumbnailMap.put(cid, img.getImageUrl());
        }
        // display_order = 1인 혜택
        List<CardBenefit> topBenefits = cardBenefitMapper.findTop1ByCardIds(cardIds);
        Map<Long, String> topBenefitMap = topBenefits.stream()
                .collect(Collectors.toMap(CardBenefit::getCardId, CardBenefit::getDisplayText, (e, r) -> e));

        final String reason = recommendReason;
        return cards.stream()
        	    .map(card -> CardListResponse.builder()
        	        .cardId(card.getCardId())
        	        .cardName(card.getCardName())
        	        .companyName(card.getCompanyName())
        	        .cardType(card.getCardType())
        	        .annualFeeDomestic(card.getAnnualFeeDomestic())   // ← 추가
        	        .thumbnailUrl(thumbnailMap.get(card.getCardId()))
        	        .topBenefit(topBenefitMap.get(card.getCardId()))
        	        .recommendReason(reason)
        	        .build())
        	    .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // 카드 상세 조회 + 조회수 증가
    // ────────────────────────────────────────────────────────────────
    @Transactional
    @Loggable(eventType = "CARD_VIEW", targetType = "CARD", 
    actionDetail = "상세조회", cardIdParam = "cardId")
    public CardDetailResponse getCardDetail(Long cardId) {

        Card card = cardMapper.findById(cardId);
        if (card == null) {
            throw new IllegalArgumentException("존재하지 않는 카드입니다. cardId=" + cardId);
        }

        // 조회수 +1
        cardMapper.incrementViewCount(cardId);

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
                .annualFeeDomestic(card.getAnnualFeeDomestic())
                .annualFeeOverseas(card.getAnnualFeeOverseas())
                .summaryDescription(card.getSummaryDescription())
                .benefits(benefits)
                .images(imageDtos)
                .contents(contentDtos)
                .termsFiles(termsFileDtos)
                .build();
    }

    // ────────────────────────────────────────────────────────────────
    // 카드 비교 (2~3개)
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    @Loggable(eventType = "CARD_COMPARE", targetType = "CARD", 
    actionDetail = "카드비교", cardIdParam = "")
    public List<CardCompareResponse> compareCards(@Valid CardCompareRequest request) {

        List<Long> cardIds = request.getCardIds();

        // ① 혜택 한 번에 조회 후 그룹핑
        List<CardBenefit> allBenefits = cardBenefitMapper.findByCardIds(cardIds);
        Map<Long, List<CardBenefit>> benefitMap = allBenefits.stream()
                .collect(Collectors.groupingBy(CardBenefit::getCardId));

        // ② 썸네일 이미지 (THUMBNAIL 우선, 없으면 FRONT 폴백, 단건 루프)
        Map<Long, String> thumbnailMap = new HashMap<>();
        for (Long cid : cardIds) {
            CardImage img = cardImageMapper.findByCardIdAndType(cid, "THUMBNAIL");
            if (img == null) img = cardImageMapper.findByCardIdAndType(cid, "FRONT");
            if (img != null) thumbnailMap.put(cid, img.getImageUrl());
        }

        return cardIds.stream()
                .map(cardId -> {
                    Card card = cardMapper.findById(cardId);
                    if (card == null) return null;

                    List<BenefitDto> benefitDtos = benefitMap
                            .getOrDefault(cardId, Collections.emptyList())
                            .stream()
                            .map(b -> BenefitDto.builder()
                                    .benefitId(b.getBenefitId())
                                    .benefitTitle(b.getBenefitTitle())
                                    .benefitType(b.getBenefitType())
                                    .discountRate(b.getDiscountRate())
                                    .cashbackRate(b.getCashbackRate())
                                    .monthlyLimitAmount(b.getMonthlyLimitAmount())
                                    .displayText(b.getDisplayText())
                                    .displayOrder(b.getDisplayOrder())
                                    .categoryName(b.getCategoryName())
                                    .iconCode(b.getIconCode())
                                    .build())
                            .collect(Collectors.toList());

                    return CardCompareResponse.builder()
                            .cardId(card.getCardId())
                            .cardName(card.getCardName())
                            .companyName(card.getCompanyName())
                            .cardType(card.getCardType())
                            .annualFeeDomestic(card.getAnnualFeeDomestic())
                            .annualFeeOverseas(card.getAnnualFeeOverseas())
                            .thumbnailUrl(thumbnailMap.get(cardId))
                            .benefits(benefitDtos)
                            .build();
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }
    // ────────────────────────────────────────────────────────────────
    // 혜택 시뮬레이션
    // MIN(monthlyAmount * rate, monthlyLimitAmount)
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SimulationResponse> simulateBenefits(@Valid CardSimulationRequest request) {

        List<Long> cardIds = request.getCardIds();
        Map<Long, Long> categoryAmounts = request.getCategoryAmounts(); // categoryId → 월지출(원)

        // 혜택 한 번에 조회
        List<CardBenefit> allBenefits = cardBenefitMapper.findByCardIds(cardIds);
        Map<Long, List<CardBenefit>> benefitMap = allBenefits.stream()
                .collect(Collectors.groupingBy(CardBenefit::getCardId));

        // 카드명 조회용 Map (비교 응답과 달리 카드명이 SimulationResponse에 있음)
        // findByCardIds가 CardMapper에 없으므로 cardId별로 단건 조회
        // → 3개 이하라 N+1 허용
        return cardIds.stream()
                .map(cardId -> {
                    Card card = cardMapper.findById(cardId);
                    if (card == null) return null;

                    List<CardBenefit> benefits = benefitMap.getOrDefault(cardId, Collections.emptyList());

                    // 카테고리별 혜택 금액 계산
                    List<SimulationResponse.BenefitBreakdown> breakdowns = benefits.stream()
                            .filter(b -> b.getCategoryId() != null)
                            .map(b -> {
                                long monthlyAmount = categoryAmounts.getOrDefault(b.getCategoryId(), 0L);
                                long benefitAmount = calcBenefit(b, monthlyAmount);
                                return SimulationResponse.BenefitBreakdown.builder()
                                        .categoryName(b.getCategoryName())
                                        .benefitAmount(benefitAmount)
                                        .build();
                            })
                            .filter(bd -> bd.getBenefitAmount() > 0)  // 0원 혜택 제외
                            .collect(Collectors.toList());

                    long totalBenefit = breakdowns.stream()
                            .mapToLong(SimulationResponse.BenefitBreakdown::getBenefitAmount)
                            .sum();

                    return SimulationResponse.builder()
                            .cardId(card.getCardId())
                            .cardName(card.getCardName())
                            .totalBenefitAmount(totalBenefit)
                            .benefitBreakdown(breakdowns)
                            .build();
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────
    // private 헬퍼
    // ────────────────────────────────────────────────────────────────

    /**
     * MIN(monthlyAmount * rate, monthlyLimitAmount)
     * benefitType에 따라 discountRate / cashbackRate / pointRate 우선순위 적용
     */
    private long calcBenefit(CardBenefit b, long monthlyAmount) {
        if (monthlyAmount == 0) return 0L;

        // 최소 결제금액 조건 체크
        if (b.getMinimumPaymentAmount() != null && monthlyAmount < b.getMinimumPaymentAmount()) {
            return 0L;
        }

        BigDecimal rate = null;
        switch (b.getBenefitType()) {
            case "RATE_DISCOUNT": rate = b.getDiscountRate();  break;
            case "CASHBACK":      rate = b.getCashbackRate();  break;
            case "POINT":         rate = b.getPointRate();     break;
            case "FIXED_DISCOUNT":
                // rate가 없고 고정 금액
                long fixed = b.getDiscountAmount() != null ? b.getDiscountAmount() : 0L;
                return applyMonthlyLimit(fixed, b.getMonthlyLimitAmount());
            case "FREE":
                return 0L;  // 무료제공은 금액 계산 불필요
            default:
                rate = b.getDiscountRate();
        }

        if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) return 0L;

        long raw = (long) (monthlyAmount * rate.doubleValue());
        return applyMonthlyLimit(raw, b.getMonthlyLimitAmount());
    }

    private long applyMonthlyLimit(long amount, Long limit) {
        if (limit != null && limit > 0) {
            return Math.min(amount, limit);
        }
        return amount;
    }

    private void saveSearchLog(String q, Long userId, int resultCount) {
        if (q == null || q.isBlank()) return;
        SearchLog log = SearchLog.builder()
                .userId(userId)
                .keywordRaw(q)
                .resultCount(resultCount)
                .build();
        searchLogMapper.insertSearchLog(log);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}