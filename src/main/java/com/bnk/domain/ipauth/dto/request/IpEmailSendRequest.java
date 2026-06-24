package com.bnk.domain.ipauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/auth/ip-verify/email/send */
@Getter
@NoArgsConstructor
public class IpEmailSendRequest {
    @NotNull  private Long   userId;
    @NotBlank private String challengeToken;
}
