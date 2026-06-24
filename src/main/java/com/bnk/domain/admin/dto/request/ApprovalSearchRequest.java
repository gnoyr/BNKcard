package com.bnk.domain.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalSearchRequest {

    private String statusCode;
    private String requestTypeCode;
    private Long   approverAdminId;  // 추가 — 현재 로그인 관리자 ID 필터용

    private int page = 0;
    private int size = 20;

    public int getOffset() { return page * size; }
}