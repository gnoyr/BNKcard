package com.bnk.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/auth/verify-email-link — 매직링크 원터치 인증 */
@Getter
@NoArgsConstructor
public class VerifyEmailLinkRequest {
    @NotBlank
    private String token;
}
