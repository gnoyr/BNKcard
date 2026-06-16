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
 * [수정 이유] 기존: residentFront(주민번호 앞 6자리) + genderCode(성별코드 1자리) →
 * IpVerifyService.verifyCi()가 CiValueGenerator.generate(name, birthDate,
 * phone)로 CI를 재생성·비교하는 방식으로 변경되면서 기존 필드로는 동작 불가
 *
 * 변경: ip-verify.js / ip-verify.html 이 실제로 전송하는 필드(name, birthDate, phone)로 일치
 *
 * [프론트엔드 전송 필드 — ip-verify.js confirmCi()] { userId, challengeToken, name,
 * birthDate, phone, nickname }
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

	/**
	 * 생년월일 YYYY-MM-DD ip-verify.js의 normalizeBirthDate()가 "19900101" → "1990-01-01"
	 * 변환 후 전송
	 */
	@NotBlank(message = "생년월일을 입력해 주세요.")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 YYYY-MM-DD 형식으로 입력해 주세요.")
	private String birthDate;

	/** 전화번호 — 포맷 무관 (IpVerifyService 내부에서 숫자만 추출·정규화) */
	@NotBlank(message = "전화번호를 입력해 주세요.")
	@Pattern(regexp = "^[0-9\\-]{9,13}$", message = "올바른 전화번호를 입력해 주세요.")
	private String phone;

	/** 기기 별명 (선택, null 허용 → 서비스에서 "내 기기" 기본값 적용) */
	@Size(max = 50)
	private String nickname;
}