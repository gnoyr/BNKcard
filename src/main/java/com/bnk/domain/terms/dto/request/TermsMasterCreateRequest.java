package com.bnk.domain.terms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermsMasterCreateRequest {
    @NotBlank
    private String termsType;   // COMMON / PRIVACY / CARD_SERVICE
    @NotBlank
    private String title;
    private String description;
    private Integer displayOrder;
    private String requiredYn;
}