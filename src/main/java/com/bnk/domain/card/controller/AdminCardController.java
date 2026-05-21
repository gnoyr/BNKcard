package com.bnk.domain.card.controller;

import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.CardCreateRequest;
import com.bnk.domain.card.dto.request.CardUpdateRequest;
import com.bnk.domain.card.service.AdminCardService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * 카드 수정 (RQ-B05).
     * 기존 CARDS 직접 수정 금지. CARD_VERSIONS snapshot + APPROVAL_REQUESTS 신규 생성.
     */
    @PutMapping("/{cardId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> updateCard(
            @PathVariable("cardId") Long cardId,
            @RequestBody @Valid CardUpdateRequest request,
            @AuthenticationPrincipal CustomAdminDetails ad) {
        return ApiResponse.toOk(adminCardService.updateCard(cardId, request, ad.getAdminId()));
    }
}
