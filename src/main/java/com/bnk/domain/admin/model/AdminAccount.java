package com.bnk.domain.admin.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccount {

    private Long          adminId;
    private String        username;
    private String        passwordHash;
    private String        name;
    private String        email;
    private String        phone;
    private String        statusCode;   // ACTIVE / INACTIVE / LOCKED
    private Long          roleId;
    private String        roleCode;     // SUPER_ADMIN / MANAGER / OPERATOR
    private String        roleName;
    private Long          createdBy;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}