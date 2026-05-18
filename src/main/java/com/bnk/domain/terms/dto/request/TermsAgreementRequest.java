package com.bnk.domain.terms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Getter
    @NoArgsConstructor
    public static class AgreedTermsItem {

        @NotNull(message = "약관 ID는 필수입니다.")
        private Long termsId;

        @NotBlank(message = "동의 여부는 필수입니다.")
        private String agreedYn;        // Y / N
    }
}
