package com.bnk.domain.account.service;

import com.bnk.domain.account.dto.request.AccountCreateRequest;
import com.bnk.domain.account.dto.response.AccountCreateResponse;
import com.bnk.domain.account.mapper.AccountMapper;
import com.bnk.domain.account.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper   accountMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 계좌 개설
     */
    @Transactional
    public AccountCreateResponse createAccount(AccountCreateRequest request, Long userId) {

        // 1. 계좌번호 채번 (102-XXXXXXX-XX)
        String accountNumber = generateAccountNumber();

        // 2. 계좌 저장
        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .accountAlias(request.getAccountAlias())
                .accountStatus("ACTIVE")
                .build();

        accountMapper.insertAccount(account);

        // 3. 비밀번호 별도 저장 (BCrypt 해시)
        String hashedPw = passwordEncoder.encode(request.getPassword());
        accountMapper.insertAccountPassword(account.getAccountId(), hashedPw);

        log.info("[Account] 계좌 개설 완료: userId={}, accountId={}, accountNumber={}",
                userId, account.getAccountId(), accountNumber);

        return AccountCreateResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(accountNumber)
                .accountType(account.getAccountType())
                .accountAlias(account.getAccountAlias())
                .accountStatus("ACTIVE")
                .createdAt(account.getCreatedAt() != null
                        ? account.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * 내 계좌 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Account> getMyAccounts(Long userId) {
        return accountMapper.findByUserId(userId);
    }

    // ── private ───────────────────────────────────────────────────

    /**
     * 계좌번호 채번 — 102-XXXXXXX-XX
     * 102: 부산은행 코드
     * XXXXXXX: 7자리 시퀀스
     * XX: 체크섬 2자리
     */
    private String generateAccountNumber() {
        Long seq = accountMapper.nextAccountSeq();
        String bankCode = "102";
        String seqStr   = String.format("%07d", seq);
        String checksum = String.format("%02d", (seq % 97));  // 간단 체크섬
        return bankCode + "-" + seqStr + "-" + checksum;
    }
}