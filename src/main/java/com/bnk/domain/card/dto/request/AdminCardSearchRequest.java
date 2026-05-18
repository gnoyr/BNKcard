package com.bnk.domain.card.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class AdminCardSearchRequest {

    private String cardName;
    private String companyName;
    private String cardType;
    private String cardStatus;
    private String brandName;
    private LocalDateTime publishStartFrom;
    private LocalDateTime publishStartTo;
    private String keyword;             // SEARCH_KEYWORDS JOIN

    private int page = 0;
    private int size = 20;
    private String sort = "createdAt"; // createdAt / cardName / annualFee / applicationCount

    public int getOffset() { return page * size; }
}
