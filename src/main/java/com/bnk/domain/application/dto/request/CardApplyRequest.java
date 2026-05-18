package com.bnk.domain.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CardApplyRequest {

    @NotNull(message = "신청 한도는 필수입니다.")
    @Min(value = 0)
    private Long requestedLimit;

    @NotBlank(message = "신청 채널은 필수입니다.")
    private String applyChannel;        // WEB / MOBILE

    /** CARD_APPLY 패키지 약관 동의 목록 */
    @NotEmpty(message = "약관 동의 목록은 필수입니다.")
    @Valid
    private List<AgreedTermsItem> agreedTerms;

    @Getter
    @NoArgsConstructor
    public static class AgreedTermsItem {
        @NotNull
        private Long termsId;
        @NotBlank
        private String agreedYn;
    }
}
