package com.bnk.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminAccountResponse {
    private Long         adminId;
    private String       username;
    private String       name;
    private String       email;
    private String       phone;
    private String       statusCode;
    private String       roleCode;    // SUPER_ADMIN / MANAGER / OPERATOR
    private String       roleName;
    private List<String> permissions; // 보유 권한 코드 목록
    private String       createdAt;
    private String       lastLoginAt;
}