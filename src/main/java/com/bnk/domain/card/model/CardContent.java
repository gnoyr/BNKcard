package com.bnk.domain.card.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardContent {
    private Long contentId;
    private Long cardId;
    private String contentType;     // INTRO / GUIDE / NOTICE / FAQ / EVENT
    private String title;
    private String contentHtml;
    private String mobileContentHtml;
    private Integer displayOrder;
    private String visibleYn;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
