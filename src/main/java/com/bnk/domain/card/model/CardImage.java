package com.bnk.domain.card.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CARD_IMAGES 테이블 단일 모델 (구 model.CardImage + model2.CardImage 통합)
 *
 * 변경 이력:
 *  - model2.CardImage Bean Validation 어노테이션 흡수
 *  - model2.CardImage 삭제 대상: com.bnk.domain.card.model2.CardImage
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardImage {

    private Long imageId;
    private Long cardId;        // @NotNull 제거
    private String imageType;   // @NotBlank, @Pattern 제거
    private String imageUrl;    // @NotBlank, @Size 제거
    private String originalName;
    private String storedName;
    private Long fileSize;
    private String mimeType;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer sortOrder;  // @NotNull, @Min, @Builder.Default 제거
    private LocalDateTime createdAt;
}
