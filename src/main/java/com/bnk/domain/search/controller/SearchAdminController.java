package com.bnk.domain.search.controller;

import com.bnk.domain.search.dto.request.KeywordCreateRequest;
import com.bnk.domain.search.dto.request.KeywordMappingRequest;
import com.bnk.domain.search.dto.request.SearchStatsRequest;
import com.bnk.domain.search.model.SearchKeyword;
import com.bnk.domain.search.service.SearchAdminService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class SearchAdminController {

    private final SearchAdminService searchAdminService;

    /**
     * B-08 검색 키워드 등록 (RQ-B09).
     * SEARCH_KEYWORDS INSERT. keyword UNIQUE 제약 체크.
     */
    @PostMapping("/keywords")
    public ResponseEntity<ApiResponse<SearchKeyword>> createKeyword(
            @RequestBody @Valid KeywordCreateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toCreated(searchAdminService.createKeyword(request, ad.getAdminId()));
    }

    /**
     * B-09 카드-키워드 매핑 (RQ-B09).
     * CARD_KEYWORDS INSERT. UNIQUE(card_id, keyword_id) 중복 무시.
     */
    @PostMapping("/cards/{cardId}/keywords")
    public ResponseEntity<ApiResponse<Integer>> mapKeywordsToCard(
            @PathVariable Long cardId,
            @RequestBody @Valid KeywordMappingRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        int mappedCount = searchAdminService.mapKeywordsToCard(cardId, request, ad.getAdminId());
        return ApiResponse.toOk(mappedCount);
    }

    /**
     * B-10 검색 로그 통계 조회 (RQ-B10).
     * 기간별 검색 횟수 / avgResultCount / nullMatchRate.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSearchStats(
            @ModelAttribute @Valid SearchStatsRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(searchAdminService.getSearchStats(request));
    }
}