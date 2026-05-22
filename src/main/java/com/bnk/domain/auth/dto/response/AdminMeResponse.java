package com.bnk.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/admin/auth/me 응답 DTO
 * header.js 의 관리자 인증 확인 전용 엔드포인트 응답.
 */
@Getter
@Builder
public class AdminMeResponse {

    private final Long         adminId;
    private final String       name;
    private final List<String> roles;  // ["ROLE_SUPER_ADMIN", "ROLE_CARD_MANAGER", ...]
}
