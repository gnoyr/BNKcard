package com.bnk.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FindIdResponse {
    private final String maskedEmail;   // ab***@domain.com
    private final String message;       // "이메일 조회 완료"
}
