package com.bnk.domain.search.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardImageMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.search.dto.response.PopularKeywordResponse;
import com.bnk.domain.search.mapper.SearchKeywordMapper;
import com.bnk.domain.search.mapper.SearchLogMapper;
import com.bnk.domain.search.model.SearchKeyword;
import com.bnk.domain.search.model.SearchLog;
import com.bnk.global.response.PageResponse;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class SearchService {

	private final SearchKeywordMapper searchKeywordMapper;
	private final SearchLogMapper searchLogMapper;
	private final CardMapper cardMapper;
	private final CardImageMapper cardImageMapper;
	private final CardBenefitMapper cardBenefitMapper;

    /**
     * F-13 카드 검색
     */
    @Transactional
    public PageResponse<CardListResponse> search(String q, Long userId, int page, int size) {

        CardSearchRequest request = new CardSearchRequest();
        request.setQ(q);
        request.setPage(page);
        request.setSize(size);

        long totalCount = cardMapper.countAll(request);

        searchLogMapper.insertSearchLog(SearchLog.builder()
                .userId(userId)
                .keywordRaw(q)
                .resultCount((int) totalCount)
                .build());

        if (totalCount == 0) {
            return PageResponse.of(Collections.emptyList(), 0L, page, size);
        }

        List<Card> cards = cardMapper.findAll(request);
        List<Long> cardIds = cards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        // THUMBNAIL 우선, 없으면 FRONT 폴백
        List<CardImage> thumbnails =
                cardImageMapper.findByCardIdsAndType(cardIds, "THUMBNAIL");

        Map<Long, String> thumbnailMap = new HashMap<>(thumbnails.stream()
                .collect(Collectors.toMap(
                        CardImage::getCardId,
                        CardImage::getImageUrl,
                        (e, r) -> e)));

        List<Long> noThumbnailIds = cardIds.stream()
                .filter(id -> !thumbnailMap.containsKey(id))
                .collect(Collectors.toList());

        if (!noThumbnailIds.isEmpty()) {
            cardImageMapper.findByCardIdsAndType(noThumbnailIds, "FRONT")
                    .forEach(img -> thumbnailMap.putIfAbsent(img.getCardId(), img.getImageUrl()));
        }

        // TOP1 혜택
        List<CardBenefit> topBenefits = cardBenefitMapper.findTop1ByCardIds(cardIds);
        Map<Long, String> topBenefitMap = topBenefits.stream()
                .collect(Collectors.toMap(
                        CardBenefit::getCardId,
                        CardBenefit::getDisplayText,
                        (e, r) -> e));

        List<CardListResponse> content = cards.stream()
                .map(card -> CardListResponse.builder()
                        .cardId(card.getCardId())
                        .cardName(card.getCardName())
                        .companyName(card.getCompanyName())
                        .cardType(card.getCardType())
                        .annualFeeDomestic(card.getAnnualFeeDomestic())
                        .annualFeeOverseas(card.getAnnualFeeOverseas())
                        .thumbnailUrl(thumbnailMap.get(card.getCardId()))
                        .topBenefit(topBenefitMap.get(card.getCardId()))
                        .build())
                .collect(Collectors.toList());

        return PageResponse.of(content, totalCount, page, size);
    }

    /** F-14 추천 검색어 */
    @Transactional(readOnly = true)
    public List<SearchKeyword> getSuggestKeywords() {
        return searchKeywordMapper.findSuggestKeywords();
    }

    /** F-15 인기 검색어 TOP10 */
    @Transactional(readOnly = true)
    public List<PopularKeywordResponse> getPopularKeywords() {
        List<Map<String, Object>> rows = searchLogMapper.findPopularKeywords(10);
        return rows.stream()
                .map(row -> PopularKeywordResponse.builder()
                        .keyword((String) row.get("KEYWORD"))
                        .searchCount(((Number) row.get("SEARCHCOUNT")).longValue())
                        .build())
                .collect(Collectors.toList());
    }
}
