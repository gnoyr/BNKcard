package com.bnk.domain.terms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TermsCreateRequest {

    @NotNull(message = "약관 마스터 ID는 필수입니다.")
    private Long termsMasterId;

    @NotBlank(message = "버전은 필수입니다.")
    @Size(max = 20)
    private String version;

    
    private String contentHtml;

    @NotNull
    private String requiredYn;              // Y / N

    private String reconsentRequiredYn;     // Y / N (기본 N)

    @NotNull(message = "적용 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
    
    //프론트에서 changeSummary 전송하므로 필드 추가
    private String changeSummary;
}
