package com.bnk.domain.search.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KeywordCreateRequest {

    @NotBlank(message = "키워드는 필수입니다.")
    @Size(max = 100)
    private String keyword;

    private Long categoryId;

    private Integer displayOrder;

    @NotNull
    @Pattern(regexp = "^[YN]$", message = "useYn 은 Y 또는 N 이어야 합니다.")
    private String useYn;
}
