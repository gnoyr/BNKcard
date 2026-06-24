package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 콘텐츠 개별 등록 요청 DTO
 *
 * 변경 이력:
 *  - Bean Validation 추가
 */
@Getter
@NoArgsConstructor
public class ContentCreateRequest {

    @NotBlank(message = "콘텐츠 유형은 필수입니다.")
    @Pattern(regexp = "INTRO|GUIDE|NOTICE",
             message = "콘텐츠 유형은 INTRO, GUIDE, NOTICE 중 하나여야 합니다.")
    private String contentType;

    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    private String contentHtml;         // CLOB
    private String mobileContentHtml;   // CLOB

    @Min(value = 1, message = "노출 순서는 1 이상이어야 합니다.")
    private Integer displayOrder;

    @Pattern(regexp = "Y|N", message = "노출 여부는 Y 또는 N이어야 합니다.")
    private String visibleYn = "Y";
}
