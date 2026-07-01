package com.bnk.domain.deviceauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/device-verify/ci
 * 본인확인(CI) 정보로 새 기기를 신뢰 등록한다.
 * userId는 challengeToken(불투명)에서 서버가 도출한다.
 */
@Getter
@NoArgsConstructor
public class DeviceCiVerifyRequest {

    @NotBlank
    private String challengeToken;

    /** 이름 — 회원가입 시 입력값과 동일해야 CI 일치 */
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요.")
    private String name;

    /** 주민번호 앞 6자리(= 생년월일 YYMMDD) */
    @NotBlank(message = "주민번호 앞 6자리를 입력해 주세요.")
    @Pattern(regexp = "^[0-9]{6}$", message = "주민번호 앞 6자리를 정확히 입력해 주세요.")
    private String residentFront;

    /** 전화번호 */
    @NotBlank(message = "전화번호를 입력해 주세요.")
    @Size(max = 20)
    private String phone;

    /** 기기 별명 override (선택) */
    @Size(max = 100)
    private String deviceName;
}
