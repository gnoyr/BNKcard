package com.bnk.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountCreateRequest {

    @NotBlank(message = "계좌 종류는 필수입니다.")
    @Pattern(regexp = "CHECKING|SAVINGS|DEPOSIT",
             message = "계좌 종류는 CHECKING, SAVINGS, DEPOSIT 중 하나여야 합니다.")
    private String accountType;       // CHECKING(입출금) / SAVINGS(적금) / DEPOSIT(예금)

    private String accountAlias;      // 계좌 별명 (선택)

    @NotBlank(message = "출금 비밀번호는 필수입니다.")
    @Pattern(regexp = "\\d{4,6}", message = "비밀번호는 4~6자리 숫자여야 합니다.")
    private String password;          // 출금 비밀번호 (4~6자리)
}