package com.bnk.domain.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(max = 50)
    private String name;

    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phone;

    @Size(max = 50)
    private String job;

    @Size(max = 50)
    private String incomeLevelCode;

    @Min(value = 300, message = "신용점수는 300 이상이어야 합니다.")
    @Max(value = 900, message = "신용점수는 900 이하여야 합니다.")
    private Integer creditScore;

    private Boolean pushEnabled;

    private Boolean marketingAgree;

    /**
     * 현재 비밀번호 재확인 — 어떤 필드든 하나 이상 변경 시 필수.
     * null 또는 빈 값이면 서비스에서 INVALID_INPUT (C001) 예외 발생.
     * BCrypt 불일치 시 INVALID_PASSWORD (U003) 예외 발생.
     */
    private String currentPassword;
}
