package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 카테고리 등록 요청 DTO
 *
 * 변경 이력:
 *  - 패키지 이동: dto.request2 → dto.request
 *  - Bean Validation 추가 (model.CardCategory와 일치)
 *  - 삭제 대상: com.bnk.domain.card.dto.request2.CategoryCreateRequest
 */
@Getter
@NoArgsConstructor
public class CategoryCreateRequest {

    @NotBlank(message = "카테고리 코드는 필수입니다.")
    @Size(max = 50, message = "카테고리 코드는 50자 이하여야 합니다.")
    private String categoryCode;

    @NotBlank(message = "카테고리명은 필수입니다.")
    @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
    private String categoryName;

    @Size(max = 100, message = "아이콘 코드는 100자 이하여야 합니다.")
    private String iconCode;

    @Min(value = 1, message = "노출 순서는 1 이상이어야 합니다.")
    private Integer displayOrder;

    @Pattern(regexp = "Y|N", message = "사용 여부는 Y 또는 N이어야 합니다.")
    private String useYn = "Y";
}
