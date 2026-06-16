package com.bnk.domain.account.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    private Long          accountId;
    private Long          userId;
    private String        accountNumber;   // 채번된 계좌번호
    private String        accountType;     // CHECKING / SAVINGS / DEPOSIT
    private String        accountAlias;    // 계좌 별명
    private String        accountStatus;   // ACTIVE / DORMANT / CLOSED
    private BigDecimal    balance;         // 잔액
    private LocalDateTime createdAt;
}