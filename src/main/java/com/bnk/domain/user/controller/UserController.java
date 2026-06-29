package com.bnk.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserCardUpdateRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.OwnedCardDetailResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.service.UserCardService;
import com.bnk.domain.user.service.UserService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.bnk.domain.user.dto.response.MonthlySpendingResponse;
import org.springframework.web.bind.annotation.RequestParam;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserCardService userCardService;

    private void requireAuth(CustomUserDetails ud) {
        if (ud == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /* 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        return ApiResponse.toOk(userService.getMyInfo(ud.getUserId()));
    }

    /**
     * 내 정보 수정
     * phone 변경 시 request.currentPassword BCrypt 검증 후 저장
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyInfo(
            @RequestBody @Valid UserUpdateRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        userService.updateMyInfo(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }

    /* 주소 변경 → CI(연계정보) 갱신 (본인인증 모달 결과) */
    @PatchMapping("/me/ci")
    public ResponseEntity<ApiResponse<Void>> updateCi(
            @RequestBody @Valid com.bnk.domain.user.dto.request.UserCiUpdateRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        userService.updateCiByAddress(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }

    /* 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        userService.changePassword(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }

    /* 보유 카드 및 신청 현황 */
    @GetMapping("/me/cards")
    public ResponseEntity<ApiResponse<CardStatusResponse>> getMyCards(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        return ApiResponse.toOk(userService.getMyCards(ud.getUserId()));
    }

    /* 보유 카드 단건 상세 (카드 관리 화면) */
    @GetMapping("/me/cards/{userCardId}")
    public ResponseEntity<ApiResponse<OwnedCardDetailResponse>> getOwnedCard(
            @PathVariable Long userCardId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        return ApiResponse.toOk(userCardService.getOwnedCard(ud.getUserId(), userCardId));
    }

    /* 보유 카드 부분 수정 (한도·해외/비접촉·별칭·상태 등) */
    @PatchMapping("/me/cards/{userCardId}")
    public ResponseEntity<ApiResponse<OwnedCardDetailResponse>> updateOwnedCard(
            @PathVariable Long userCardId,
            @RequestBody @Valid UserCardUpdateRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        return ApiResponse.toOk(
                userCardService.updateOwnedCard(ud.getUserId(), userCardId, request));
    }
    
    /** 월별 카드별 이용금액 집계 */
    @GetMapping("/me/monthly-spending")
    public ResponseEntity<ApiResponse<MonthlySpendingResponse>> getMonthlySpending(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);

        // year/month 미입력 시 현재 년월로 자동 설정
        if (year == 0 || month == 0) {
            java.time.LocalDate now = java.time.LocalDate.now();
            year  = now.getYear();
            month = now.getMonthValue();
        }

        return ApiResponse.toOk(userService.getMonthlySpending(ud.getUserId(), year, month));
    }
    
    /** 푸시 토큰 등록 */
    @PutMapping("/me/push-token")
    public ResponseEntity<ApiResponse<Void>> registerPushToken(
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        userService.updatePushToken(ud.getUserId(), body.get("pushToken"));
        return ApiResponse.toOk(null);
    }

    /** 푸시 토큰 해제 */
    @DeleteMapping("/me/push-token")
    public ResponseEntity<ApiResponse<Void>> clearPushToken(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        userService.clearPushToken(ud.getUserId());
        return ApiResponse.toOk(null);
    }
    
}