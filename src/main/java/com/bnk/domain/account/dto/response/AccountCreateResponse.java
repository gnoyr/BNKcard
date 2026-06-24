package com.bnk.domain.account.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountCreateResponse {
    private Long   accountId;
    private String accountNumber;   // 채번된 계좌번호 (예: 102-0000001-88)
    private String accountType;
    private String accountAlias;
    private String accountStatus;
    private String createdAt;
}