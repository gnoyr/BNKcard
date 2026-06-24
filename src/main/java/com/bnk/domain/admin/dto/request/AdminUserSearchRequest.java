package com.bnk.domain.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserSearchRequest {

    private String name;
    private String email;
    private String phone;
    private String statusCode;          // ACTIVE / SUSPENDED / DORMANT / WITHDRAWN
    private LocalDate birthDateFrom;
    private LocalDate birthDateTo;
    private int page = 0;
    private int size = 20;

    public int getOffset() { return page * size; }
}
