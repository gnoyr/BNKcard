package com.bnk.domain.card.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardImage {
    private Long imageId;
    private Long cardId;
    private String imageType;   // FRONT / BACK / THUMBNAIL / DETAIL
    private String imageUrl;
    private String originalName;
    private String storedName;
    private Long fileSize;
    private String mimeType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
