package com.bnk.domain.card.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.BenefitUpdateRequest;
import com.bnk.domain.card.dto.request.CardCreateRequest;
import com.bnk.domain.card.dto.request.CardStatusRequest;
import com.bnk.domain.card.dto.request.CardUpdateRequest;
import com.bnk.domain.card.dto.request.ContentUpdateRequest;
import com.bnk.domain.card.dto.request.ImageUpdateRequest;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.service.AdminCardService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.response.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
public class AdminCardController {

    private final AdminCardService adminCardService;

    /**
     * 관리자 카드 목록 다중조건 검색 (RQ-B13).
     * MyBatis <if> 동적 SQL. 페이지네이션 + 정렬.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<?>>> getAdminCardList(
            @ModelAttribute AdminCardSearchRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminCardService.getAdminCardList(request, ad.getAdminId()));
    }
    
    /**
     * 관리자 카드 상세 조회.
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardDetailResponse>> getCardDetail(
            @PathVariable Long cardId,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminCardService.getAdminCardDetail(cardId));
    }

    /**
     * 카드 신규 등록 (RQ-B04).
     * CARDS INSERT(DRAFT) + CARD_VERSIONS INSERT(snapshot) + APPROVAL_REQUESTS INSERT — @Transactional
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Long>>> createCard(
            @RequestBody @Valid CardCreateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toCreated(adminCardService.createCard(request, ad.getAdminId()));
    }

    /**
     * 카드 기본정보 수정 (RQ-B05).
     * 기존 CARDS 직접 수정 금지. CARD_VERSIONS snapshot + APPROVAL_REQUESTS 신규 생성.
     */
    @PutMapping("/{cardId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> updateCard(
            @PathVariable Long cardId,
            @RequestBody @Valid CardUpdateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminCardService.updateCard(cardId, request, ad.getAdminId()));
    }
    
    // 혜택 등록/수정
    @PutMapping("/{cardId}/benefits")
    public ResponseEntity<ApiResponse<Map<String, Long>>> saveCardBenefits(
            @PathVariable Long cardId,
            @RequestBody @Valid BenefitUpdateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminCardService.saveCardBenefits(cardId, request, ad.getAdminId()));
    }

    // 이미지 등록/수정
    @PutMapping("/{cardId}/images")
    public ResponseEntity<ApiResponse<Map<String, Long>>> saveCardImages(
            @PathVariable Long cardId,
            @RequestBody @Valid ImageUpdateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminCardService.saveCardImages(cardId, request, ad.getAdminId()));
    }
    
    // 콘텐츠 등록/수정
    @PutMapping("/{cardId}/contents")
    public ResponseEntity<ApiResponse<Void>> saveCardContents(
            @PathVariable Long cardId,
            @RequestBody @Valid ContentUpdateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        adminCardService.saveCardContents(cardId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }
    
    /**
     * 카드 상태 강제 변경 (APPROVED→PUBLISHED, PUBLISHED→STOPPED 등).
     * 스케줄러 외 수동 처리 및 긴급 중지에 사용.
     * 허용 상태: APPROVED, PUBLISHED, STOPPED, EXPIRED
     */
    @PatchMapping("/{cardId}/status")
    public ResponseEntity<ApiResponse<Void>> changeCardStatus(
            @PathVariable Long cardId,
            @RequestBody @Valid CardStatusRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        adminCardService.changeCardStatus(cardId, request, ad.getAdminId());
        return ApiResponse.toOk(null);
    }
}
