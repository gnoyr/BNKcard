package com.bnk.domain.ipauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/auth/ip-verify/email/confirm */
@Getter
@NoArgsConstructor
public class IpEmailConfirmRequest {
    @NotNull  private Long   userId;
    @NotBlank private String challengeToken;
    @NotBlank @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.") private String code;
    @Size(max = 50) private String nickname;  // null 허용 — '내 기기' 기본값
}
