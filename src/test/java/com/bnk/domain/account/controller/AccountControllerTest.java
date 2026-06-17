package com.bnk.domain.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bnk.domain.account.dto.response.AccountCreateResponse;
import com.bnk.domain.account.model.Account;
import com.bnk.domain.account.service.AccountService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.exception.GlobalExceptionHandler;
import com.bnk.global.util.audit.AuditLogger;

/**
 * AccountController 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 수정 이력 ─────────────────────────────────────────────────────────
 * · @MockitoSettings(LENIENT) 추가
 *   → Bean Validation 실패 케이스는 서비스 호출 전에 400 반환되므로
 *     mockUserDetails.getUserId() stub이 실제로 호출되지 않아
 *     UnnecessaryStubbingException 발생 → LENIENT로 해결
 * · 비로그인 케이스: standaloneSetup은 Security 필터 없음
 *   → null principal → NPE(500) → is5xxServerError() 로 검증
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AccountController 단위 테스트")
class AccountControllerTest {

    @Mock private AccountService  accountService;
    @Mock private AuditLogger     auditLogger;

    @InjectMocks private AccountController accountController;

    private MockMvc mvc;
    private CustomUserDetails mockUserDetails;

    private static final Long USER_ID = 1L;
    private static final Long ACCT_ID = 100L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockUserDetails = Mockito.mock(CustomUserDetails.class);
        given(mockUserDetails.getUserId()).willReturn(USER_ID);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter p) {
                return p.hasParameterAnnotation(AuthenticationPrincipal.class);
            }
            @Override
            public Object resolveArgument(MethodParameter p, ModelAndViewContainer mvc,
                                          NativeWebRequest req, WebDataBinderFactory binder) {
                return req.getHeader("X-No-Auth") != null ? null : mockUserDetails;
            }
        };

        mvc = MockMvcBuilders.standaloneSetup(accountController)
                .setValidator(validator)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogger))
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // POST /api/accounts — 계좌 개설
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("계좌 개설 [POST /api/accounts]")
    class CreateAccount {

        private AccountCreateResponse successResp() {
            return AccountCreateResponse.builder()
                    .accountId(ACCT_ID)
                    .accountNumber("102-0000001-01")
                    .accountType("SAVINGS")
                    .accountAlias("월급통장")
                    .accountStatus("ACTIVE")
                    .build();
        }

        @Test
        @DisplayName("[정상] 201 Created — accountId·accountNumber 반환")
        void 정상_계좌개설_201() throws Exception {
            given(accountService.createAccount(any(), anyLong())).willReturn(successResp());

            mvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "accountType": "SAVINGS",
                              "accountAlias": "월급통장",
                              "password": "1234"
                            }
                            """))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.accountId").value(ACCT_ID))
                    .andExpect(jsonPath("$.data.accountNumber").value("102-0000001-01"))
                    .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
        }

        @Test
        @DisplayName("[실패] accountType 누락 → 400 BAD_REQUEST (Bean Validation)")
        void 실패_accountType_누락_400() throws Exception {
            // Bean Validation 실패 → 서비스 미호출 → userId stub 불필요
            mvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "password": "1234"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[실패] accountType 패턴 불일치(SAVING) → 400 BAD_REQUEST")
        void 실패_accountType_패턴불일치_400() throws Exception {
            mvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "accountType": "SAVING",
                              "password": "1234"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[실패] password 누락 → 400 BAD_REQUEST (Bean Validation)")
        void 실패_password_누락_400() throws Exception {
            mvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "accountType": "SAVINGS"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[실패] 잘못된 JSON → 400 BAD_REQUEST")
        void 실패_잘못된JSON_400() throws Exception {
            mvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[실패] 비로그인(X-No-Auth) → principal null → NPE(5xx)")
        void 실패_비로그인_에러() throws Exception {
            mvc.perform(post("/api/accounts")
                    .header("X-No-Auth", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "accountType": "SAVINGS",
                              "password": "1234"
                            }
                            """))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("[실패] 서비스 예외 → GlobalExceptionHandler 처리")
        void 실패_서비스예외_처리() throws Exception {
            willThrow(new BusinessException(ErrorCode.INVALID_INPUT))
                    .given(accountService).createAccount(any(), anyLong());

            mvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "accountType": "SAVINGS",
                              "password": "1234"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // GET /api/accounts/me — 내 계좌 목록 조회
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("내 계좌 목록 조회 [GET /api/accounts/me]")
    class GetMyAccounts {

        private Account accountFixture(Long accountId, String accountNumber) {
            Account a = Account.builder()
                    .userId(USER_ID)
                    .accountNumber(accountNumber)
                    .accountType("SAVINGS")
                    .accountAlias("통장")
                    .accountStatus("ACTIVE")
                    .build();
            ReflectionTestUtils.setField(a, "accountId", accountId);
            return a;
        }

        @Test
        @DisplayName("[정상] 계좌 2건 → 200 OK + JSON 배열 반환")
        void 정상_계좌목록_200() throws Exception {
            given(accountService.getMyAccounts(USER_ID)).willReturn(List.of(
                    accountFixture(100L, "102-0000001-01"),
                    accountFixture(101L, "102-0000002-02")
            ));

            mvc.perform(get("/api/accounts/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("[정상] 계좌 없음 → 200 OK + 빈 배열")
        void 정상_계좌없음_빈배열_200() throws Exception {
            given(accountService.getMyAccounts(USER_ID)).willReturn(Collections.emptyList());

            mvc.perform(get("/api/accounts/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("[실패] 비로그인(X-No-Auth) → principal null → NPE(5xx)")
        void 실패_비로그인_에러() throws Exception {
            mvc.perform(get("/api/accounts/me")
                    .header("X-No-Auth", "true"))
                    .andExpect(status().is5xxServerError());
        }
    }
}