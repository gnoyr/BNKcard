package com.bnk.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50)
    private String name;

    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phone;               // 변경 시 is_phone_verified='N' 강제

    @Size(max = 50)
    private String job;

    @Size(max = 50)
    private String incomeLevelCode;

    private Boolean pushEnabled;

    private Boolean marketingAgree;
}
