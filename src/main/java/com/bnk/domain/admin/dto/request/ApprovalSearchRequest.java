package com.bnk.domain.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalSearchRequest {

    private String statusCode;          // PENDING / APPROVED / REJECTED
    private String requestTypeCode;     // CARD_PUBLISH / CARD_UPDATE / TERMS_PUBLISH
    private int page = 0;
    private int size = 20;

    public int getOffset() { return page * size; }
}
