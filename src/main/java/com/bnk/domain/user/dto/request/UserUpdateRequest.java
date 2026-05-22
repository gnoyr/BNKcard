package com.bnk.domain.user.dto.request;

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

    private Boolean pushEnabled;

    private Boolean marketingAgree;

    /**
     * 비밀번호 재확인 (phone 변경 시에만 필수)
     * 서버에서 phone != null 이면 이 필드가 있는지 검증
     */
    private String currentPassword;
}
