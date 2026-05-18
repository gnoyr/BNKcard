package com.bnk.domain.user.controller;

import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.service.UserService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회.
     * phone·email 마스킹, password_hash·ci_value 제외.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(userService.getMyInfo(ud.getUserId()));
    }

    /**
     * 내 정보 수정.
     * phone 변경 시 is_phone_verified='N' 강제 + AUDIT_LOGS INSERT.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyInfo(
            @RequestBody @Valid UserUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        userService.updateMyInfo(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }

    /**
     * 비밀번호 변경 (로그인 중).
     * currentPassword BCrypt 검증 → newPassword 저장 → 타 기기 세션 revoke.
     */
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        userService.changePassword(ud.getUserId(), request);
        return ApiResponse.toOk(null);
    }
}
