package com.bnk.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminCreateRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String email;
    private String phone;

    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "MANAGER|OPERATOR", message = "역할은 MANAGER 또는 OPERATOR만 가능합니다.")
    private String roleCode;  // SUPER_ADMIN 생성은 별도 API
}