package com.bnk.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doAnswer;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.account.dto.request.AccountCreateRequest;
import com.bnk.domain.account.dto.response.AccountCreateResponse;
import com.bnk.domain.account.mapper.AccountMapper;
import com.bnk.domain.account.model.Account;

/**
 * AccountService 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 핵심 주의 ────────────────────────────────────────────────────────
 * · insertAccount(Account) → void 반환
 *   → given().willAnswer() 불가 (void 메서드에 given() 사용 불가)
 *   → doAnswer(...).when(mock).method() 사용 (Mockito BDD 아닌 일반 스타일)
 * · insertAccountPassword(Long, String) → void 반환
 *   → willDoNothing().given() 사용
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AccountService 단위 테스트")
class AccountServiceTest {

    @Mock private AccountMapper   accountMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private static final Long   USER_ID       = 1L;
    private static final Long   ACCT_ID       = 100L;
    private static final String ACCOUNT_TYPE  = "SAVINGS";
    private static final String ACCOUNT_ALIAS = "월급통장";
    private static final String RAW_PW        = "1234";
    private static final String ENC_PW        = "$2a$encoded";

    /**
     * insertAccount는 void 반환 + MyBatis selectKey로 accountId 채번.
     * doAnswer로 Account.accountId 필드를 직접 세팅하여 시뮬레이션.
     */
    private void stubInsertAccount(Long returnAccountId) {
        doAnswer(inv -> {
            Account a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "accountId", returnAccountId);
            return null; // void
        }).when(accountMapper).insertAccount(any(Account.class));
    }

    private AccountCreateRequest createReq(String accountType, String alias, String pw) {
        AccountCreateRequest req = new AccountCreateRequest();
        ReflectionTestUtils.setField(req, "accountType",  accountType);
        ReflectionTestUtils.setField(req, "accountAlias", alias);
        ReflectionTestUtils.setField(req, "password",     pw);
        return req;
    }

    private Account accountFixture(Long accountId) {
        Account a = Account.builder()
                .userId(USER_ID)
                .accountNumber("102-0000001-01")
                .accountType(ACCOUNT_TYPE)
                .accountAlias(ACCOUNT_ALIAS)
                .accountStatus("ACTIVE")
                .build();
        ReflectionTestUtils.setField(a, "accountId", accountId);
        return a;
    }

    // ════════════════════════════════════════════════════════════════
    // createAccount()
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("계좌 개설 [createAccount]")
    class CreateAccount {

        @Test
        @DisplayName("[정상] 계좌 개설 → AccountCreateResponse 반환 + insertAccount·insertAccountPassword 호출")
        void 정상_계좌개설() {
            given(accountMapper.nextAccountSeq()).willReturn(1L);
            stubInsertAccount(ACCT_ID);
            given(passwordEncoder.encode(RAW_PW)).willReturn(ENC_PW);
            willDoNothing().given(accountMapper).insertAccountPassword(anyLong(), anyString());

            AccountCreateResponse resp = accountService.createAccount(
                    createReq(ACCOUNT_TYPE, ACCOUNT_ALIAS, RAW_PW), USER_ID);

            assertThat(resp).isNotNull();
            assertThat(resp.getAccountId()).isEqualTo(ACCT_ID);
            assertThat(resp.getAccountType()).isEqualTo(ACCOUNT_TYPE);
            assertThat(resp.getAccountAlias()).isEqualTo(ACCOUNT_ALIAS);
            assertThat(resp.getAccountStatus()).isEqualTo("ACTIVE");
            assertThat(resp.getAccountNumber()).matches("102-\\d{7}-\\d{2}");

            then(accountMapper).should().insertAccount(any(Account.class));
            then(accountMapper).should().insertAccountPassword(eq(ACCT_ID), eq(ENC_PW));
        }

        @Test
        @DisplayName("[정상] 계좌번호 포맷 — seq=5 → 102-0000005-05")
        void 정상_계좌번호포맷() {
            given(accountMapper.nextAccountSeq()).willReturn(5L);
            stubInsertAccount(ACCT_ID);
            given(passwordEncoder.encode(any())).willReturn(ENC_PW);
            willDoNothing().given(accountMapper).insertAccountPassword(anyLong(), anyString());

            AccountCreateResponse resp = accountService.createAccount(
                    createReq(ACCOUNT_TYPE, ACCOUNT_ALIAS, RAW_PW), USER_ID);

            assertThat(resp.getAccountNumber()).isEqualTo("102-0000005-05");
        }

        @Test
        @DisplayName("[정상] 체크섬 경계 — seq=97 → checksum=00")
        void 정상_체크섬경계_seq97() {
            given(accountMapper.nextAccountSeq()).willReturn(97L);
            stubInsertAccount(ACCT_ID);
            given(passwordEncoder.encode(any())).willReturn(ENC_PW);
            willDoNothing().given(accountMapper).insertAccountPassword(anyLong(), anyString());

            AccountCreateResponse resp = accountService.createAccount(
                    createReq(ACCOUNT_TYPE, ACCOUNT_ALIAS, RAW_PW), USER_ID);

            assertThat(resp.getAccountNumber()).endsWith("-00");
        }

        @Test
        @DisplayName("[정상] BCrypt 인코딩 — 원본 비밀번호로 encode 호출 검증")
        void 정상_BCrypt_파라미터검증() {
            given(accountMapper.nextAccountSeq()).willReturn(1L);
            stubInsertAccount(ACCT_ID);
            given(passwordEncoder.encode(RAW_PW)).willReturn(ENC_PW);
            willDoNothing().given(accountMapper).insertAccountPassword(anyLong(), anyString());

            accountService.createAccount(createReq(ACCOUNT_TYPE, ACCOUNT_ALIAS, RAW_PW), USER_ID);

            ArgumentCaptor<String> pwCaptor = ArgumentCaptor.forClass(String.class);
            then(passwordEncoder).should().encode(pwCaptor.capture());
            assertThat(pwCaptor.getValue()).isEqualTo(RAW_PW);
        }

        @Test
        @DisplayName("[정상] alias null 허용 — alias 없이도 계좌 개설 성공")
        void 정상_alias_null() {
            given(accountMapper.nextAccountSeq()).willReturn(10L);
            stubInsertAccount(ACCT_ID);
            given(passwordEncoder.encode(any())).willReturn(ENC_PW);
            willDoNothing().given(accountMapper).insertAccountPassword(anyLong(), anyString());

            AccountCreateResponse resp = accountService.createAccount(
                    createReq(ACCOUNT_TYPE, null, RAW_PW), USER_ID);

            assertThat(resp).isNotNull();
            assertThat(resp.getAccountAlias()).isNull();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // getMyAccounts()
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("내 계좌 목록 조회 [getMyAccounts]")
    class GetMyAccounts {

        @Test
        @DisplayName("[정상] 계좌 2건 반환")
        void 정상_계좌목록() {
            given(accountMapper.findByUserId(USER_ID)).willReturn(
                    List.of(accountFixture(100L), accountFixture(101L)));

            List<Account> result = accountService.getMyAccounts(USER_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("[정상] 계좌 없음 → 빈 리스트 반환")
        void 정상_계좌없음_빈리스트() {
            given(accountMapper.findByUserId(USER_ID)).willReturn(Collections.emptyList());

            List<Account> result = accountService.getMyAccounts(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("[정상] findByUserId 호출 시 userId 파라미터 검증")
        void 정상_userId_파라미터검증() {
            given(accountMapper.findByUserId(USER_ID)).willReturn(Collections.emptyList());

            accountService.getMyAccounts(USER_ID);

            ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
            then(accountMapper).should().findByUserId(captor.capture());
            assertThat(captor.getValue()).isEqualTo(USER_ID);
        }
    }
}