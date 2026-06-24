package com.bnk.domain.terms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 약관 동의 요청 DTO — POST /api/terms/agree
 *
 * 변경 이력:
 *  - Inner Class AgreedTermsItem 제거
 *    → com.bnk.domain.terms.dto.request.AgreedTermsItem 공통 DTO로 분리
 *    → CardApplyRequest.AgreedTermsItem 과 동일한 구조를 중복 정의하고 있었음
 */
@Getter
@NoArgsConstructor
public class TermsAgreementRequest {

    @NotBlank(message = "동의 출처는 필수입니다.")
    private String agreementSource;     // SIGNUP / CARD_APPLY / EVENT

    @NotBlank(message = "동의 채널은 필수입니다.")
    private String agreementChannel;    // WEB / MOBILE / ADMIN

    @NotEmpty(message = "약관 동의 목록은 필수입니다.")
    @Valid
    private List<AgreedTermsItem> agreedTerms;
}
