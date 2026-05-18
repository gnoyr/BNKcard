package com.bnk.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

	@NotBlank(message = "토큰을 입력해주세요.")
	private String token;

	@NotBlank(message = "새 비밀번호를 입력해주세요.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,50}$", message = "비밀번호는 8자 이상 50자 이하의 영문, 숫자, 특수문자 조합이어야 합니다.")
	private String newPassword;

	@NotBlank(message = "비밀번호 확인을 입력해주세요.")
	private String newPasswordConfirm;
}