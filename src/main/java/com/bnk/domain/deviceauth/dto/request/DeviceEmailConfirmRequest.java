package com.bnk.domain.deviceauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/auth/device-verify/email/confirm */
@Getter
@NoArgsConstructor
public class DeviceEmailConfirmRequest {
    @NotBlank private String challengeToken;
    @NotBlank @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.") private String code;
    /** 기기 별명 override (선택) — null이면 로그인 시 전달된 기기명 사용 */
    @Size(max = 100) private String deviceName;
}
