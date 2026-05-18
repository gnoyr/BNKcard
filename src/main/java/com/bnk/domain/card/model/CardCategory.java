package com.bnk.domain.card.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardCategory {
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String iconCode;
    private Integer displayOrder;
    private String useYn;
    private LocalDateTime createdAt;
}
