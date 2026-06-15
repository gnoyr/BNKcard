package com.bnk.domain.search.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.bnk.domain.search.dto.response.SearchResultResponse;
import com.bnk.domain.search.mapper.SearchKeywordMapper;
import com.bnk.domain.search.mapper.SearchLogMapper;
import com.bnk.domain.search.model.SearchKeyword;
import com.bnk.domain.search.model.SearchLog;
import com.bnk.global.response.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchKeywordMapper searchKeywordMapper;
    private final SearchLogMapper searchLogMapper;
    private final CardMapper cardMapper;
    private final CardImageMapper cardImageMapper;
    private final CardBenefitMapper cardBenefitMapper;

    // ai.enabled=false 환경 대비 optional 주입
    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    /**
     * F-13 카드 검색 — 키워드 0건 시 AI 의미 검색 + 오타 교정 fallback
     */
    @Transactional
    public SearchResultResponse search(String q, Long userId, int page, int size) {

        CardSearchRequest request = new CardSearchRequest();
        request.setQ(q);
        request.setPage(page);
        request.setSize(size);

        // 1. 기존 키워드 검색
        long totalCount = cardMapper.countAll(request);
        List<Card> cards = cardMapper.findAll(request);

        // 2. 키워드 0건 → AI fallback
        boolean aiSearched = false;
        String correctedQuery = null;
        String aiSearchMessage = null;

        if (cards.isEmpty() && q != null && !q.isBlank() && vectorStore != null) {
            log.info("[Search] 키워드 결과 0건 → AI 의미 검색 fallback: q={}", q);

            // Gemini로 오타 교정 시도
            correctedQuery = correctQuery(q);

            // 교정어로 벡터 검색 (교정 실패하면 원본 쿼리로)
            String searchQuery = (correctedQuery != null && !correctedQuery.isBlank())
                    ? correctedQuery : q;
            cards = semanticFallback(searchQuery);
            totalCount = cards.size();

            if (!cards.isEmpty()) {
                aiSearched = true;
                if (correctedQuery != null
                        && !correctedQuery.isBlank()
                        && !correctedQuery.equalsIgnoreCase(q)) {
                    // 오타 교정된 경우 — "약관으로 검색한 결과입니다"
                    aiSearchMessage = "'" + correctedQuery + "'으로 검색한 결과입니다.";
                } else {
                    // 오타는 없지만 의미 검색으로 찾은 경우
                    aiSearchMessage = "AI 의미 검색 결과입니다.";
                }
            }
        }

        // 3. 최종 결과 없으면 빈 응답
        if (cards.isEmpty()) {
            searchLogMapper.insertSearchLog(SearchLog.builder()
                    .keywordRaw(q)
                    .resultCount(0)
                    .userId(userId)
                    .build());
            return SearchResultResponse.builder()
                    .page(PageResponse.of(Collections.emptyList(), 0L, page, size))
                    .aiSearched(false)
                    .build();
        }

        // 4. 이미지 / 혜택 조회
        List<Long> cardIds = cards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        Map<Long, String> thumbnailMap = new HashMap<>();
        for (Long cid : cardIds) {
            CardImage img = cardImageMapper.findByCardIdAndType(cid, "THUMBNAIL");
            if (img == null) img = cardImageMapper.findByCardIdAndType(cid, "FRONT");
            if (img != null) thumbnailMap.put(cid, img.getImageUrl());
        }

        Map<Long, String> topBenefitMap = cardBenefitMapper.findTop1ByCardIds(cardIds)
                .stream()
                .collect(Collectors.toMap(
                        CardBenefit::getCardId, CardBenefit::getDisplayText, (a, b) -> a));

        // 5. 응답 조립
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

        searchLogMapper.insertSearchLog(SearchLog.builder()
                .keywordRaw(q)
                .resultCount((int) Math.min(totalCount, Integer.MAX_VALUE))
                .userId(userId)
                .build());

        return SearchResultResponse.builder()
                .page(PageResponse.of(content, totalCount, page, size))
                .aiSearched(aiSearched)
                .correctedQuery(correctedQuery)
                .aiSearchMessage(aiSearchMessage)
                .build();
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

    // ── Private ───────────────────────────────────────────────────────

    /**
     * Gemini로 오타 교정 — 교정 불필요하면 null 반환
     */
    private String correctQuery(String q) {
        if (chatClientBuilder == null) return null;
        try {
            ChatClient client = chatClientBuilder.build();
            String result = client.prompt()
                    .user(u -> u.text("""
                            다음 검색어의 오타만 교정해줘.
                            반드시 원본과 비슷한 단어로만 교정해.
                            완전히 다른 단어나 문장으로 바꾸면 안 돼.
                            교정된 단어 하나만 반환해. 설명 없이.
                            오타가 없거나 확실하지 않으면 원본 그대로 반환해.
                            절대로 새로운 문장을 만들지 마.
                            
                            예시:
                            - "딩댕" → "딩딩"
                            - "해외여핼" → "해외여행"
                            - "싸인카드" → "사인카드"
                            
                            검색어: {query}
                            """)
                            .param("query", q))
                    .call()
                    .content();
            
            // 교정 결과가 원본보다 2배 이상 길면 무시 (문장으로 바뀐 경우)
            if (result != null && result.trim().length() > q.length() * 2) {
                log.warn("[Search] 오타 교정 결과가 너무 길어 무시: {} → {}", q, result.trim());
                return null;
            }
            
            return result != null ? result.trim() : null;
        } catch (Exception e) {
            log.warn("[Search] 오타 교정 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Qdrant 의미 기반 검색 후 Oracle 카드 조회
     */
    private List<Card> semanticFallback(String q) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(q)
                            .topK(3)
                            .similarityThreshold(0.4)
                            .build()
            );
            
            log.warn("[SemanticFallback] Qdrant 결과: {}건", docs.size());
            
            if (docs.isEmpty()) return Collections.emptyList();

            List<Long> cardIds = docs.stream()
                    .map(d -> {
                        Object id = d.getMetadata().get("card_id");
                        log.warn("[SemanticFallback] card_id raw: {}, type: {}",
                            id, id != null ? id.getClass().getSimpleName() : "null");
                        if (id == null) return null;
                        if (id instanceof Number) return ((Number) id).longValue();
                        if (id instanceof String) {
                            try { return Long.parseLong((String) id); }
                            catch (Exception e) { return null; }
                        }
                        try { return Long.parseLong(String.valueOf(id)); }
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            log.warn("[SemanticFallback] 최종 cardIds: {}", cardIds);
            
            return cardIds.isEmpty()
                    ? Collections.emptyList()
                    : cardMapper.findByIds(cardIds);

        } catch (Exception e) {
            log.warn("[Search] AI 의미 검색 실패 (무시): {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}