package com.bnk.domain.application.dto.request;

import java.util.List;

import com.bnk.domain.terms.dto.request.AgreedTermsItem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 신청 요청 DTO — POST /api/cards/{cardId}/apply
 */
@Getter
@NoArgsConstructor
public class CardApplyRequest {

	@NotNull(message = "신청 한도는 필수입니다.")
	@Min(value = 0)
	private Long requestedLimit;

	@NotBlank(message = "신청 채널은 필수입니다.")
	private String applyChannel; // WEB / MOBILE

	/** CARD_APPLY 패키지 약관 동의 목록 */
	@NotEmpty(message = "약관 동의 목록은 필수입니다.")
	@Valid
	private List<AgreedTermsItem> agreedTerms;
}
