package com.bnk.domain.terms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 약관 개별 동의 항목 공통 DTO
 *
 * 변경 이력:
 *  - TermsAgreementRequest.AgreedTermsItem (Inner Class) 분리 추출
 *  - CardApplyRequest.AgreedTermsItem (Inner Class) 분리 추출
 *  → 두 클래스가 동일한 필드 구조(termsId, agreedYn)를 중복 정의하고 있었음
 *  → 이 클래스 하나로 통합, 두 Request 클래스에서 참조
 *
 * 사용처:
 *  - com.bnk.domain.terms.dto.request.TermsAgreementRequest
 *  - com.bnk.domain.application.dto.request.CardApplyRequest
 */
@Getter
@NoArgsConstructor
public class AgreedTermsItem {

    @NotNull(message = "약관 ID는 필수입니다.")
    private Long termsId;

    @NotBlank(message = "동의 여부는 필수입니다.")
    private String agreedYn;   // Y / N
}
