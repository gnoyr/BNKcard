package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentCreateRequest {

    @NotBlank(message = "콘텐츠 타입은 필수입니다.")
    @Pattern(regexp = "INTRO|GUIDE|NOTICE|FAQ|EVENT")
    private String contentType;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 300)
    private String title;

    private String contentHtml;
    private String mobileContentHtml;
    private Integer displayOrder;

    @Pattern(regexp = "Y|N")
    private String visibleYn;
}
