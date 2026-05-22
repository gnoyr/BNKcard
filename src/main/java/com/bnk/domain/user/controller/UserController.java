package com.bnk.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.service.UserService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** F-24 | 내 정보 조회 — phone·email 마스킹, password_hash·ci_value 제외 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(userService.getMyInfo(ud.getUserId()));
    }

    /** F-25 | 내 정보 수정 — phone 변경 시 is_phone_verified='N' + AUDIT_LOGS INSERT */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyInfo(
            @RequestBody @Valid UserUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        userService.updateMyInfo(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }

    /** F-26 | 비밀번호 변경 — BCrypt 검증 → 신규 저장 → 전체 세션 revoke */
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        userService.changePassword(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }

    /**
     * RQ-F17 | 보유 카드 및 신청 현황 조회
     * USER_CARDS(deleted_yn='N') + CARD_APPLICATIONS
     * ※ 소비 패턴(GET·PUT /api/users/me/spending)은 SpendingController에서 담당
     */
    @GetMapping("/me/cards")
    public ResponseEntity<ApiResponse<CardStatusResponse>> getMyCards(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(userService.getMyCards(ud.getUserId()));
    }
}