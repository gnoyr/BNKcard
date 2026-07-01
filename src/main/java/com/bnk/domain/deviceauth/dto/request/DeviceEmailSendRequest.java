package com.bnk.domain.deviceauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/device-verify/email/send
 * userId는 challengeToken(불투명)에서 서버가 도출하므로 클라이언트가 보내지 않는다.
 */
@Getter
@NoArgsConstructor
public class DeviceEmailSendRequest {
    @NotBlank private String challengeToken;
}
