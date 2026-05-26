package com.bnk.domain.card.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardCategory2 {
    private Long   categoryId;
    private String categoryCode;
    private String categoryName;
    private String iconCode;
    private Integer displayOrder;
}