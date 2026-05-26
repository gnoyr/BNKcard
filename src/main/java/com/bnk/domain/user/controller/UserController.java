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
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private void requireAuth(CustomUserDetails ud) {
        if (ud == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /** F-24 | 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        return ApiResponse.toOk(userService.getMyInfo(ud.getUserId()));
    }

    /**
     * F-25 | 내 정보 수정
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

    /** F-26 | 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        userService.changePassword(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }

    /** RQ-F17 | 보유 카드 및 신청 현황 */
    @GetMapping("/me/cards")
    public ResponseEntity<ApiResponse<CardStatusResponse>> getMyCards(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud) {
        requireAuth(ud);
        return ApiResponse.toOk(userService.getMyCards(ud.getUserId()));
    }
}