package com.bnk.domain.account.controller;

import com.bnk.domain.account.dto.request.AccountCreateRequest;
import com.bnk.domain.account.dto.response.AccountCreateResponse;
import com.bnk.domain.account.model.Account;
import com.bnk.domain.account.service.AccountService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 계좌 개설
     * POST /api/accounts
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountCreateResponse>> createAccount(
            @RequestBody @Valid AccountCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toCreated(
                accountService.createAccount(request, ud.getUserId()));
    }

    /**
     * 내 계좌 목록 조회
     * GET /api/accounts/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<Account>>> getMyAccounts(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(
                accountService.getMyAccounts(ud.getUserId()));
    }
}