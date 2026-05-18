package com.bnk.domain.auth.dto.request;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

	@NotBlank(message = "이메일을 입력해주세요.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.")
	private String email;

	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,50}$", message = "비밀번호는 8자 이상 50자 이하의 영문, 숫자, 특수문자 조합이어야 합니다.")
	private String password;

	@NotBlank(message = "이름을 입력해주세요.")
	@Size(max = 50)
	private String name;

	@NotBlank(message = "휴대폰 번호를 입력해주세요.")
	@Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
	private String phone;

	@Pattern(regexp = "^(19|20)\\d{6}$", message = "생년월일은 yyyyMMdd 형식으로 입력해주세요.")
	private String birthDate; // yyyyMMdd → 서비스단 LocalDate 변환

	private String marketingAgree; // Y / N (선택)

	@NotEmpty(message = "약관 동의 목록은 필수입니다.")
	private List<Long> agreedTermsIds;
}