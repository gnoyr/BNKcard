package com.bnk.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.auth.dto.request.AdminLoginRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    /** 관리자 로그인 — Access + Refresh 쿠키 발급 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> adminLogin(
            @RequestBody @Valid AdminLoginRequest request,
            HttpServletResponse response) {
        AuthTokenResult result = authService.adminLogin(request);
        response.addHeader(HttpHeaders.SET_COOKIE, result.getAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString());
        return ResponseEntity.ok(ApiResponse.message("관리자 로그인에 성공했습니다."));
    }
}