package com.bnk.domain.ipauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/auth/ip-verify/ci */
@Getter
@NoArgsConstructor
public class IpCiVerifyRequest {
    @NotNull  private Long   userId;
    @NotBlank private String challengeToken;
    @NotBlank @Pattern(regexp = "^\\d{6}$",  message = "주민번호 앞 6자리를 입력해 주세요.") private String residentFront;
    @NotBlank @Pattern(regexp = "^[1-4]$",   message = "성별 코드가 올바르지 않습니다.")    private String genderCode;
    @Size(max = 50) private String nickname;
}
