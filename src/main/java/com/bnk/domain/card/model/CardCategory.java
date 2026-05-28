package com.bnk.domain.card.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CARD_CATEGORIES 테이블 단일 모델
 *
 * 변경 이력:
 *  - model2.CardCategory Bean Validation 흡수
 *  - createdAt 타입: java.util.Date → java.time.LocalDateTime 통일
 *  - 삭제 대상: com.bnk.domain.card.model2.CardCategory
 *              com.bnk.domain.card.model.CardCategory2
 *  - CardCategoryMapper, CardCategoryMapper.xml은 이미 이 클래스를 참조 → 변경 불필요
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCategory {

    private Long categoryId;

    @NotBlank(message = "카테고리 코드는 필수입니다.")
    @Size(max = 50, message = "카테고리 코드는 50자 이하여야 합니다.")
    private String categoryCode;

    @NotBlank(message = "카테고리명은 필수입니다.")
    @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
    private String categoryName;

    @Size(max = 100, message = "아이콘 코드는 100자 이하여야 합니다.")
    private String iconCode;

    private Integer displayOrder;

    @NotBlank(message = "사용 여부는 필수입니다.")
    @Pattern(regexp = "Y|N", message = "사용 여부는 Y 또는 N이어야 합니다.")
    @Builder.Default
    private String useYn = "Y";

    private LocalDateTime createdAt;
}
