package com.bnk.domain.card.model2;

import java.time.LocalDateTime;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardImage {
	private Long imageId;

    @NotNull(message = "카드 ID는 필수입니다.")
    private Long cardId;                    // NUMBER(19) FK NN

    @NotBlank(message = "이미지 타입은 필수입니다.")
    @Pattern(regexp = "FRONT|BACK|THUMBNAIL|DETAIL",
             message = "이미지 타입은 FRONT, BACK, THUMBNAIL, DETAIL 중 하나여야 합니다.")
    private String imageType;               // VARCHAR2(30) NN

    @NotBlank(message = "이미지 URL은 필수입니다.")
    @Size(max = 1000, message = "이미지 URL은 1000자 이하여야 합니다.")
    private String imageUrl;                // VARCHAR2(1000) NN

    @Size(max = 300, message = "원본 파일명은 300자 이하여야 합니다.")
    private String originalName;            // VARCHAR2(300) NULL

    @Size(max = 300, message = "저장 파일명은 300자 이하여야 합니다.")
    private String storedName;              // VARCHAR2(300) NULL

    @Min(value = 0, message = "파일 크기는 0 이상이어야 합니다.")
    private Long fileSize;                  // NUMBER(15) NULL (bytes)

    @Size(max = 100, message = "MIME 타입은 100자 이하여야 합니다.")
    private String mimeType;               // VARCHAR2(100) NULL

    @Min(value = 0, message = "이미지 너비는 0 이상이어야 합니다.")
    private Integer imageWidth;            // NUMBER(5) NULL (px)

    @Min(value = 0, message = "이미지 높이는 0 이상이어야 합니다.")
    private Integer imageHeight;           // NUMBER(5) NULL (px)

    @NotNull(message = "노출 순서는 필수입니다.")
    @Min(value = 1, message = "노출 순서는 1 이상이어야 합니다.")
    private Integer sortOrder = 1;         // NUMBER(5) DEFAULT 1 NN

    private LocalDateTime createdAt;       // TIMESTAMP DEFAULT SYSTIMESTAMP NN
}
