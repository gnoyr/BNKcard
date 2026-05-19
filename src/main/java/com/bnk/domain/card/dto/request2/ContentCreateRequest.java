package com.bnk.domain.card.dto.request2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentCreateRequest {

    private Long    cardId;
    private String  contentType;    // INTRO / GUIDE / NOTICE / FAQ / EVENT
    private String  title;
    private String  contentHtml;
    private String  mobileContentHtml;
    private Integer displayOrder;
    private String  visibleYn;
}
