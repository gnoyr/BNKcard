package com.bnk.domain.admin.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {
    private Long adminId;
    private String username;
    private String passwordHash;        // 응답 DTO에서 제외
    private String name;
    private String email;
    private String phone;
    private String statusCode;          // ACTIVE / SUSPENDED
    private Integer loginFailCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String deletedYn;
    // ADMIN_USER_ROLES → ADMIN_ROLES JOIN 결과
    private List<String> roleCodes;
}
