package com.bnk.domain.terms.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermsMasterResponse {
    private Long   termsMasterId;
    private String termsType;   // COMMON / PRIVACY / CARD_SERVICE
    private String title;
    private String description;
}