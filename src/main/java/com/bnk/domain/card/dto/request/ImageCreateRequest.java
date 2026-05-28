package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 이미지 개별 등록 요청 DTO
 *
 * 변경 이력:
 *  - Bean Validation 추가 (model.CardImage validation과 일치)
 */
@Getter
@NoArgsConstructor
public class ImageCreateRequest {

    @NotBlank(message = "이미지 타입은 필수입니다.")
    @Pattern(regexp = "FRONT|BACK|THUMBNAIL|DETAIL",
             message = "이미지 타입은 FRONT, BACK, THUMBNAIL, DETAIL 중 하나여야 합니다.")
    private String imageType;

    @NotBlank(message = "이미지 URL은 필수입니다.")
    @Size(max = 1000, message = "이미지 URL은 1000자 이하여야 합니다.")
    private String imageUrl;

    @Size(max = 300)
    private String originalName;

    @Size(max = 300)
    private String storedName;

    @Min(value = 0)
    private Long fileSize;

    @Size(max = 100)
    private String mimeType;

    @Min(value = 0)
    private Integer imageWidth;

    @Min(value = 0)
    private Integer imageHeight;

    @NotNull(message = "노출 순서는 필수입니다.")
    @Min(value = 1, message = "노출 순서는 1 이상이어야 합니다.")
    private Integer sortOrder;
}
