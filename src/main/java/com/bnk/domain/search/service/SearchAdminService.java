package com.bnk.domain.search.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.search.dto.request.KeywordCreateRequest;
import com.bnk.domain.search.dto.request.KeywordMappingRequest;
import com.bnk.domain.search.dto.request.SearchStatsRequest;
import com.bnk.domain.search.mapper.SearchKeywordMapper;
import com.bnk.domain.search.mapper.SearchLogMapper;
import com.bnk.domain.search.model.SearchKeyword;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class SearchAdminService {

    private final SearchKeywordMapper searchKeywordMapper;
    private final SearchLogMapper searchLogMapper;

    /**
     * B-08 검색 키워드 등록 (RQ-B09)
     * SEARCH_KEYWORDS INSERT (keyword UNIQUE 체크 포함)
     */
    @Transactional
    public SearchKeyword createKeyword(@Valid KeywordCreateRequest request, Long adminId) {
        // keyword 중복 체크
        searchKeywordMapper.findByKeyword(request.getKeyword())
                .ifPresent(k -> { throw new BusinessException(ErrorCode.DUPLICATE_KEYWORD); });

        SearchKeyword keyword = SearchKeyword.builder()
                .keyword(request.getKeyword())
                .categoryId(request.getCategoryId())
                .useYn(request.getUseYn())
                .displayOrder(request.getDisplayOrder())
                .createdBy(adminId)
                .build();

        searchKeywordMapper.insertKeyword(keyword); // keyProperty=keywordId 자동 주입
        log.info("[키워드등록] keywordId={}, keyword={}", keyword.getKeywordId(), keyword.getKeyword());
        return keyword;
    }

    /**
     * B-09 카드-키워드 매핑 (RQ-B09)
     * CARD_KEYWORDS INSERT (UNIQUE(card_id, keyword_id) 중복 무시)
     * @return 실제 매핑된 건수
     */
    @Transactional
    public int mapKeywordsToCard(Long cardId, @Valid KeywordMappingRequest request, Long adminId) {
        int count = 0;
        for (Long keywordId : request.getKeywordIds()) {
            // keywordId 존재 여부 확인
            searchKeywordMapper.findById(keywordId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

            // 중복이면 NOT EXISTS로 건너뜀 (XML에서 처리)
            int inserted = searchKeywordMapper.insertCardKeyword(cardId, keywordId, adminId);
            count += inserted;
        }
        log.info("[카드-키워드매핑] cardId={}, 요청={}건, 실매핑={}건",
                cardId, request.getKeywordIds().size(), count);
        return count;
    }

    /**
     * B-10 검색 로그 통계 조회 (RQ-B10)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSearchStats(SearchStatsRequest request) {
        return searchLogMapper.findSearchStats(request);
    }
}