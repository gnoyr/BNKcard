package com.bnk.domain.card.model2;

import lombok.*;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardContent {
	
	private Long contentId;

    @NotNull(message = "카드 ID는 필수입니다.")
    private Long cardId;                    // NUMBER(19) FK NN

    @NotBlank(message = "콘텐츠 타입은 필수입니다.")
    @Pattern(regexp = "INTRO|GUIDE|NOTICE|FAQ|EVENT",
             message = "콘텐츠 타입은 INTRO, GUIDE, NOTICE, FAQ, EVENT 중 하나여야 합니다.")
    private String contentType;             // VARCHAR2(50) NN

    @NotBlank(message = "콘텐츠 제목은 필수입니다.")
    @Size(max = 300, message = "콘텐츠 제목은 300자 이하여야 합니다.")
    private String title;                   // VARCHAR2(300) NN
    
    private String contentHtml;            // CLOB NULL
    private String mobileContentHtml;      // CLOB NULL
    
    private Integer displayOrder;          // NUMBER(5) NULL

    @NotBlank(message = "노출 여부는 필수입니다.")
    @Pattern(regexp = "Y|N", message = "노출 여부는 Y 또는 N이어야 합니다.")
    @Builder.Default
    private String visibleYn = "Y";        // CHAR(1) DEFAULT 'Y' NN

    private Long createdBy;       // NUMBER(19) NULL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
