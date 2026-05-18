package com.bnk.domain.card.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.card.dto.request.CardCompareRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.dto.request.CardSimulationRequest;
import com.bnk.domain.card.dto.response.BannerDto;
import com.bnk.domain.card.dto.response.CardCompareResponse;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.dto.response.SimulationResponse;
import com.bnk.domain.card.service.CardService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.response.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * 홈 배너 조회.
     * 비로그인: view_count 상위 3개 CARD_PROMOTIONS.banner_image_url
     * 우수회원: USER_SPENDING_PATTERNS 최대 카테고리 → CARD_IMAGES(FRONT)
     * required=false → ud=null 이면 비로그인 처리
     */
    @GetMapping("/home/banners")
    public ResponseEntity<ApiResponse<List<BannerDto>>> getHomeBanners(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        Long userId = ud != null ? ud.getUserId() : null;
        return ApiResponse.toOk(cardService.getHomeBanners(userId));
    }

    /**
     * 카드 목록 + 검색.
     * 검색어 있으면 SEARCH_LOGS INSERT.
     */
    @GetMapping("/cards")
    public ResponseEntity<ApiResponse<PageResponse<CardListResponse>>> getCardList(
            @ModelAttribute @Valid CardSearchRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        Long userId = ud != null ? ud.getUserId() : null;
        return ApiResponse.toOk(cardService.getCardList(request, userId));
    }

    /**
     * TOP3 추천.
     * ① 비회원: view_count DESC 상위 3
     * ② 신규회원: 설문결과 기반
     * ③ 우수회원: USER_SPENDING_PATTERNS → CARD_BENEFITS JOIN
     */
    @GetMapping("/cards/top3")
    public ResponseEntity<ApiResponse<List<CardListResponse>>> getTop3Cards(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud,
            @RequestParam(required = false) String surveyResult) {
        Long userId = ud != null ? ud.getUserId() : null;
        return ApiResponse.toOk(cardService.getTop3Cards(userId, surveyResult));
    }

    /**
     * 카드 상세 조회.
     * CARDS.view_count++ UPDATE.
     */
    @GetMapping("/cards/{cardId}")
    public ResponseEntity<ApiResponse<CardDetailResponse>> getCardDetail(
            @PathVariable Long cardId) {
        return ApiResponse.toOk(cardService.getCardDetail(cardId));
    }

    /**
     * 카드 비교 (최대 3개).
     * CARD_BENEFITS JOIN, 시뮬레이션 선택.
     */
    @PostMapping("/cards/compare")
    public ResponseEntity<ApiResponse<List<CardCompareResponse>>> compareCards(
            @RequestBody @Valid CardCompareRequest request) {
        return ApiResponse.toOk(cardService.compareCards(request));
    }

    /**
     * 혜택 시뮬레이션.
     * MIN(monthlyAmount * discountRate, monthlyLimitAmount) 공식.
     */
    @PostMapping("/cards/simulate")
    public ResponseEntity<ApiResponse<List<SimulationResponse>>> simulateBenefits(
            @RequestBody @Valid CardSimulationRequest request) {
        return ApiResponse.toOk(cardService.simulateBenefits(request));
    }
}
