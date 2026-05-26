package com.bnk.domain.card.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CardSearchRequest {

    private String q;               // 검색어
    private String cardType;        // CREDIT / CHECK / HYBRID
    private String companyName;
    private Long categoryId;
    private List<Long> categoryIds;  // 다중 선택용 (교집합)
    private Long minFee;
    private Long maxFee;
    private int page = 0;
    private int size = 20;

    public int getOffset() { return page * size; }
}
