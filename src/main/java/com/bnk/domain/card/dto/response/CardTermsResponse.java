package com.bnk.domain.card.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardTermsResponse {
    private Long termsId;
    private String title;
    private String version;
    private String requiredYn;
    private Integer displayOrder;
}