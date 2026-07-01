package com.bnk.domain.ipauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/ip-verify/ci 요청 DTO
 *
 * [변경 이력]
 *  v1: residentFront + genderCode (단순 연결 방식, CI와 불일치)
 *  v2: name + birthDate + phone (CiValueGenerator v1 기준)
 *  v3: name + residentFront + genderCode + address (CiValueGenerator v2 기준)
 *
 * [프론트엔드 전송 필드 — ip-verify.js confirmCi()]
 *  { userId, challengeToken, name, residentFront, genderCode, address, nickname }
 */
@Getter
@NoArgsConstructor
public class IpCiVerifyRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String challengeToken;

    /** 이름 — 회원가입 시 입력값과 동일해야 CI 일치 */
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요.")
    private String name;

    /** 주민번호 앞 6자리(= 생년월일 YYMMDD) — 회원가입 시 입력값과 동일해야 CI 일치 */
    @NotBlank(message = "주민번호 앞 6자리를 입력해 주세요.")
    @Pattern(regexp = "^[0-9]{6}$", message = "주민번호 앞 6자리를 정확히 입력해 주세요.")
    private String residentFront;

    /** 전화번호 — 회원가입 시 입력값과 동일해야 CI 일치 */
    @NotBlank(message = "전화번호를 입력해 주세요.")
    @Size(max = 20)
    private String phone;

    /** 기기 별명 (선택, null 허용 → 서비스에서 "내 기기" 기본값 적용) */
    @Size(max = 50)
    private String nickname;
}