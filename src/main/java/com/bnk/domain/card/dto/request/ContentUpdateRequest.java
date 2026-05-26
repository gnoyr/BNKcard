package com.bnk.domain.card.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class ContentUpdateRequest {

    @Valid
    private List<ContentCreateRequest> contents;
}