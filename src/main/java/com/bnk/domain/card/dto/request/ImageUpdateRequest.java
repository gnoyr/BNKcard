package com.bnk.domain.card.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class ImageUpdateRequest {
    @NotBlank(message = "수정 사유는 필수입니다.")
    private String changeSummary;

    @Valid
    private List<ImageCreateRequest> images;

}
