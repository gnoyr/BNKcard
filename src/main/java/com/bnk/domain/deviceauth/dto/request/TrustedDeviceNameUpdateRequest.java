package com.bnk.domain.deviceauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** PATCH /api/users/me/trusted-devices/{deviceTrustId} */
@Getter
@NoArgsConstructor
public class TrustedDeviceNameUpdateRequest {
    @NotBlank(message = "기기 이름은 빈 문자열일 수 없습니다.")
    @Size(max = 100, message = "기기 이름은 100자 이내로 입력해 주세요.")
    private String deviceName;
}
