package com.bnk.domain.search.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchKeyword {
    private Long keywordId;
    private String keyword;
    private Long categoryId;
    private String useYn;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private String deletedYn;
    private LocalDateTime deletedAt;
}
