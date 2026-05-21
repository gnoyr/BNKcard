package com.bnk.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 링크 요청 DTO (F-22)
 *
 */
@Getter
@NoArgsConstructor
public class FindPasswordRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;
}