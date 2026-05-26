package com.bnk.domain.card.model;

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
	private Long cardId;
	
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
	@Min(value = 1)
	private Integer sortOrder;

    private LocalDateTime createdAt;
}
