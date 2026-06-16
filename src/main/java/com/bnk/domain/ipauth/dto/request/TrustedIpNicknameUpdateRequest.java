package com.bnk.domain.ipauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** PATCH /api/users/me/trusted-ips/{trustId} */
@Getter
@NoArgsConstructor
public class TrustedIpNicknameUpdateRequest {
    @NotBlank(message = "별명은 빈 문자열일 수 없습니다.")
    @Size(max = 50, message = "별명은 50자 이내로 입력해 주세요.")
    private String nickname;
}
