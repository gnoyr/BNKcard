package com.bnk.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindIdResponse {
    private final String maskedEmail;   // ab***@domain.com
    private final String message;       // "이메일 조회 완료"
}
