package com.bnk.domain.card.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class MerchantCategoryMap {
    private Long mapId;
    private String keyword;
    private Long categoryId;
    private Integer priority;
    private String useYn;
    private LocalDateTime createdAt;
}
