package com.bnk.domain.search.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.search.dto.response.PopularKeywordResponse;
import com.bnk.domain.search.dto.response.SearchResponse;
import com.bnk.domain.search.model.SearchKeyword;
import com.bnk.domain.search.service.SearchService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 카드 검색 (RQ-F08, F13).
     * SEARCH_KEYWORDS LIKE → CARD_KEYWORDS JOIN + CARDS.card_name LIKE UNION.
     * SEARCH_LOGS INSERT 필수 (비로그인 허용 → userId nullable).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SearchResponse>>> search(
            @RequestParam String q,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long userId = ud != null ? ud.getUserId() : null;
        
        // 서비스 메서드에 page와 size를 직접 넘겨주도록 변경합니다.
        return ApiResponse.toOk(searchService.search(q, userId, page, size));
    }

    /**
     * 추천 검색어 (RQ-F09).
     * SEARCH_KEYWORDS WHERE use_yn='Y' ORDER BY display_order ASC LIMIT 10.
     */
    @GetMapping("/keywords/suggest")
    public ResponseEntity<ApiResponse<List<SearchKeyword>>> getSuggestKeywords() {
        return ApiResponse.toOk(searchService.getSuggestKeywords());
    }

    /**
     * 인기 검색어 TOP10 (RQ-F10).
     * SEARCH_LOGS 최근 7일 GROUP BY keyword_raw COUNT DESC LIMIT 10.
     */
    @GetMapping("/keywords/popular")
    public ResponseEntity<ApiResponse<List<PopularKeywordResponse>>> getPopularKeywords() {
        return ApiResponse.toOk(searchService.getPopularKeywords());
    }
}