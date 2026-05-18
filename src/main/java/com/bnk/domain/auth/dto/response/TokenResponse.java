package com.bnk.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private final String accessToken;
    private final String tokenType;    // "Bearer"
    private final Long expiresIn;      // 초 단위 (7200)
    private final Long userId;
    private final String role;
    // refreshToken 은 HttpOnly 쿠키로 전달 — 응답 바디 미포함
}
