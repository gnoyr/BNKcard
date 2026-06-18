package com.bnk.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminRoleUpdateRequest {

    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "MANAGER|OPERATOR", message = "역할은 MANAGER 또는 OPERATOR만 가능합니다.")
    private String roleCode;
}