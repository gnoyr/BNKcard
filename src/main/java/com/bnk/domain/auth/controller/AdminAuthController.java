package com.bnk.domain.auth.controller;

import com.bnk.domain.auth.dto.request.AdminLoginRequest;
import com.bnk.domain.auth.dto.response.TokenResponse;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    /**
     * 관리자 로그인.
     * ADMIN_USER_ROLES → ADMIN_ROLES.role_code 를 JWT Claim에 포함.
     * Refresh Token → HttpOnly 쿠키.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> adminLogin(
            @RequestBody @Valid AdminLoginRequest request,
            HttpServletResponse response) {

        var result = authService.adminLogin(request);
        response.addHeader(HttpHeaders.SET_COOKIE, result.getCookie().toString());
        return ApiResponse.toOk(result.getToken());
    }
}
