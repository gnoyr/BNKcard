package com.bnk.domain.auth.dto.request;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

	// ── 필수 ──────────────────────────────────────────────────────────
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

	@NotEmpty(message = "약관 동의 목록은 필수입니다.")
	private List<Long> agreedTermsIds;

	// ── 선택 ──────────────────────────────────────────────────────────

	@Pattern(regexp = "^(19|20)\\d{2}-?(0[1-9]|1[0-2])-?(0[1-9]|[12]\\d|3[01])$", message = "생년월일은 yyyyMMdd 또는 yyyy-MM-dd 형식으로 입력해주세요.")
	private String birthDate;

	private Boolean marketingAgree;

	@Size(max = 50)
	private String job;

	@Size(max = 50)
	private String incomeLevelCode;

	@Min(value = 300, message = "신용점수는 300 이상이어야 합니다.")
	@Max(value = 900, message = "신용점수는 900 이하여야 합니다.")
	private Integer creditScore;

	/**
	 * 주민번호 앞 6자리 (생년월일 + 성별). 서버에서 CI값 생성 후 즉시 폐기. DB에 직접 저장 금지. 카드 신청 시 CI
	 * 검증용으로도 활용. 선택값이나 입력 시 더 정확한 CI 생성 가능.
	 */
	@Pattern(regexp = "^[0-9]{6}$", message = "주민번호 앞 6자리를 입력해주세요.")
	private String residentFront;
}
