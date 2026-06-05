package com.bnk.domain.search.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.search.dto.response.PopularKeywordResponse;
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
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<CardListResponse>>> search(@RequestParam String q,
			@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		Long userId = ud != null ? ud.getUserId() : null;
		return ApiResponse.toOk(searchService.search(q, userId, page, size));
	}

	/** 추천 검색어 (RQ-F09) */
	@GetMapping("/keywords/suggest")
	public ResponseEntity<ApiResponse<List<SearchKeyword>>> getSuggestKeywords() {
		return ApiResponse.toOk(searchService.getSuggestKeywords());
	}

	/** 인기 검색어 TOP10 (RQ-F10) */
	@GetMapping("/keywords/popular")
	public ResponseEntity<ApiResponse<List<PopularKeywordResponse>>> getPopularKeywords() {
		return ApiResponse.toOk(searchService.getPopularKeywords());
	}
}
