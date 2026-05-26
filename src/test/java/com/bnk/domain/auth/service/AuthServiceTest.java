package com.bnk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.auth.dto.request.EmailVerifyRequest;
import com.bnk.domain.auth.dto.request.FindPasswordRequest;
import com.bnk.domain.auth.dto.request.LoginRequest;
import com.bnk.domain.auth.dto.request.ResetPasswordRequest;
import com.bnk.domain.auth.dto.request.SendVerifyCodeRequest;
import com.bnk.domain.auth.dto.request.SignupRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.auth.model.UserSession;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.auth.JwtTokenProvider;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.TokenStore;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    // ── 기존 Mock ──────────────────────────────────────────────────────
    @Mock private UserMapper              userMapper;
    @Mock private UserSessionMapper       userSessionMapper;
    @Mock private PasswordEncoder         passwordEncoder;
    @Mock private JwtTokenProvider        jwtTokenProvider;
    @Mock private CookieUtil              cookieUtil;
    @Mock private TokenStore        tokenStore;

    // ── 추가 Mock (AuthService 생성자 주입에 필요) ─────────────────────
    @Mock private TermsMapper             termsMapper;
    @Mock private AdminUserMapper         adminUserMapper;
    @Mock private UserTermsAgreementMapper userTermsAgreementMapper;
    @Mock private EmailService            emailService;

    @InjectMocks
    private AuthService authService;

    // ── 공통 상수 ──────────────────────────────────────────────────────
    private static final Long   USER_ID  = 1L;
    private static final String EMAIL    = "test@bnk.co.kr";
    private static final String NAME     = "홍길동";
    private static final String PHONE    = "01012345678";
    private static final String PASSWORD = "Password123!";
    private static final String ENC_PW   = "$2a$10$encodedPasswordHashValue";
    private static final String TOKEN    = "mock-uuid-token";
    private static final String NEW_PW   = "NewSecure123!";

    // ── 공통 픽스처 ────────────────────────────────────────────────────

    /** 정상 활성 계정 (로그인 실패 횟수 0, 잠금 없음) */
    private User activeUser() {
        User user = new User();
        ReflectionTestUtils.setField(user, "userId",        USER_ID);
        ReflectionTestUtils.setField(user, "email",         EMAIL);
        ReflectionTestUtils.setField(user, "name",          NAME);
        ReflectionTestUtils.setField(user, "passwordHash",  ENC_PW);
        ReflectionTestUtils.setField(user, "statusCode",    "ACTIVE");
        ReflectionTestUtils.setField(user, "loginFailCount", 0);
        return user;
    }

    /** 로그인 실패 4회 누적 상태 (다음 실패 시 잠금 트리거) */
    private User userWithFourFails() {
        User user = activeUser();
        ReflectionTestUtils.setField(user, "loginFailCount", 4);
        return user;
    }

    /** 계정 잠금 상태 (lockedUntil = 30분 후) */
    private User lockedUser() {
        User user = activeUser();
        ReflectionTestUtils.setField(user, "lockedUntil", LocalDateTime.now().plusMinutes(30));
        return user;
    }

    /** statusCode를 동적으로 교체한 유저 */
    private User userWithStatus(String status) {
        User user = activeUser();
        ReflectionTestUtils.setField(user, "statusCode", status);
        return user;
    }

    /** HttpServletRequest 최소 Mock (IP / UA 반환) */
    private HttpServletRequest mockHttpReq() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.lenient().when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        Mockito.lenient().when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        Mockito.lenient().when(req.getHeader("User-Agent")).thenReturn("JUnit/5");
        return req;
    }

    /** 로그인 성공 시 쿠키 생성까지 필요한 공통 스텁 */
    private void stubLoginSuccess() {
        given(passwordEncoder.matches(PASSWORD, ENC_PW)).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(USER_ID, "ROLE_USER")).willReturn("acc-token");
        given(jwtTokenProvider.generateRefreshToken(USER_ID)).willReturn("ref-token");
        given(jwtTokenProvider.getRefreshExpirationSec()).willReturn(604800L);
        given(jwtTokenProvider.getAccessExpirationSec()).willReturn(7200L);
        given(cookieUtil.createAccessCookie("acc-token", 7200L))
                .willReturn(ResponseCookie.from("access_token", "acc-token").build());
        given(cookieUtil.createRefreshCookie("ref-token", 604800L))
                .willReturn(ResponseCookie.from("refresh_token", "ref-token").build());
    }

    // ── Request DTO 빌더 헬퍼 ─────────────────────────────────────────

    private LoginRequest loginReq(String email, String pw) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "email",    email);
        ReflectionTestUtils.setField(req, "password", pw);
        return req;
    }

    private SignupRequest signupReq(String email, String pw, String name,
                                    String phone, List<Long> termIds) {
        SignupRequest req = new SignupRequest();
        ReflectionTestUtils.setField(req, "email",          email);
        ReflectionTestUtils.setField(req, "password",       pw);
        ReflectionTestUtils.setField(req, "name",           name);
        ReflectionTestUtils.setField(req, "phone",          phone);
        ReflectionTestUtils.setField(req, "agreedTermsIds", termIds);
        return req;
    }

    private SendVerifyCodeRequest sendCodeReq(String email) {
        SendVerifyCodeRequest req = new SendVerifyCodeRequest();
        ReflectionTestUtils.setField(req, "email", email);
        return req;
    }

    private EmailVerifyRequest emailVerifyReq(String email, String code) {
        EmailVerifyRequest req = new EmailVerifyRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "code",  code);
        return req;
    }

    private FindPasswordRequest findPwReq(String email, String name) {
        FindPasswordRequest req = new FindPasswordRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "name",  name);
        return req;
    }

    private ResetPasswordRequest resetPwReq(String token, String pw, String confirm) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        ReflectionTestUtils.setField(req, "token",              token);
        ReflectionTestUtils.setField(req, "newPassword",        pw);
        ReflectionTestUtils.setField(req, "newPasswordConfirm", confirm);
        return req;
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-03 | 로그인
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("[정상] 올바른 계정 → Access/Refresh 쿠키 반환")
        void 정상_로그인성공() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(activeUser()));
            stubLoginSuccess();

            AuthTokenResult result = authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq());

            assertThat(result.getAccessCookie()).isNotNull();
            assertThat(result.getRefreshCookie()).isNotNull();
            then(userSessionMapper).should().insertSession(any());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 이메일 → USER_NOT_FOUND")
        void 실패_이메일없음() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 계정 잠금 상태 → ACCOUNT_LOCKED")
        void 실패_계정잠김() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(lockedUser()));

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
        }

        @Test
        @DisplayName("[실패] 정지 계정(SUSPENDED) → ACCOUNT_SUSPENDED")
        void 실패_계정정지() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithStatus("SUSPENDED")));

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED));
        }

        @Test
        @DisplayName("[실패] 탈퇴 계정(WITHDRAWN) → ACCOUNT_WITHDRAWN")
        void 실패_탈퇴계정() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithStatus("WITHDRAWN")));

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            // validateUserStatus()가 "WITHDRAWN" → ACCOUNT_WITHDRAWN 예외를 던진다
                            .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN));
        }

        @Test
        @DisplayName("[실패] 비밀번호 불일치 → INVALID_PASSWORD + incrementLoginFailCount 호출")
        void 실패_비밀번호불일치() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches(PASSWORD, ENC_PW)).willReturn(false);

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PASSWORD));

            then(userMapper).should().incrementLoginFailCount(USER_ID);
        }

        @Test
        @DisplayName("[실패] 비밀번호 5회 연속 오류 → updateLockedUntil 호출(계정 잠금)")
        void 실패_5회오류_계정잠금() {
            // 이미 4회 실패한 상태에서 1회 더 실패하면 누적 5회 → 잠금
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithFourFails()));
            given(passwordEncoder.matches(PASSWORD, ENC_PW)).willReturn(false);

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class);

            then(userMapper).should().incrementLoginFailCount(USER_ID);
            then(userMapper).should().updateLockedUntil(eq(USER_ID), any(LocalDateTime.class));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-05 | 로그아웃
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("[정상] 세션 전체 파기(revokeAllByUserId) 호출 확인")
        void 정상_로그아웃() {
            authService.logout(USER_ID);
            then(userSessionMapper).should().revokeAllByUserId(USER_ID);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-04 | Access Token 재발급
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Access Token 재발급")
    class Refresh {

        private UserSession validSession() {
            return UserSession.builder()
                    .sessionId(10L)
                    .userId(USER_ID)
                    .refreshToken("valid-refresh")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
        }

        @Test
        @DisplayName("[정상] 유효한 Refresh Token → 새 Access 쿠키 반환")
        void 정상_토큰재발급() {
            given(userSessionMapper.findByRefreshToken("valid-refresh"))
                    .willReturn(Optional.of(validSession()));
            given(jwtTokenProvider.generateAccessToken(USER_ID, "ROLE_USER")).willReturn("new-acc");
            given(jwtTokenProvider.getAccessExpirationSec()).willReturn(7200L);
            given(cookieUtil.createAccessCookie("new-acc", 7200L))
                    .willReturn(ResponseCookie.from("access_token", "new-acc").build());

            ResponseCookie result = authService.refresh("valid-refresh");

            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("new-acc");
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 Refresh Token → REFRESH_TOKEN_INVALID")
        void 실패_토큰없음() {
            given(userSessionMapper.findByRefreshToken("invalid-token")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("invalid-token"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-01 | 회원가입
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("회원가입")
    class Signup {

        /** 필수 약관 1개 + 선택 약관 1개 */
        private List<Terms> twoTerms() {
            Terms required = Terms.builder().termsId(1L).requiredYn("Y").version("1.0").build();
            Terms optional = Terms.builder().termsId(2L).requiredYn("N").version("1.0").build();
            return List.of(required, optional);
        }

        /** 이메일/전화 중복 없음 + 인증 완료 + 약관 정상 스텁 */
        private void stubSignupHappyPath() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            given(userMapper.existsByPhone(anyString())).willReturn(0);
            given(tokenStore.get("email:verified:" + EMAIL)).willReturn("Y");
            given(termsMapper.findByPackageType("SIGNUP")).willReturn(twoTerms());
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENC_PW);
        }

        @Test
        @DisplayName("[정상] 회원가입 완료 → insertUser·updateEmailVerified·insertAgreements·tokenStore.delete 호출")
        void 정상_회원가입성공() {
            stubSignupHappyPath();

            authService.signup(signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L, 2L)));

            then(userMapper).should().insertUser(any());
            then(userMapper).should().updateEmailVerified(any(), eq("Y"));
            then(userTermsAgreementMapper).should().insertAgreements(any());
            then(tokenStore).should().delete("email:verified:" + EMAIL);
        }

        @Test
        @DisplayName("[실패] 이메일 중복 → DUPLICATE_EMAIL")
        void 실패_이메일중복() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(1);

            assertThatThrownBy(() -> authService.signup(signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
        }

        @Test
        @DisplayName("[실패] 휴대폰 중복 → DUPLICATE_PHONE")
        void 실패_전화번호중복() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            given(userMapper.existsByPhone(anyString())).willReturn(1);

            assertThatThrownBy(() -> authService.signup(signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_PHONE));
        }

        @Test
        @DisplayName("[실패] 이메일 인증 미완료 → EMAIL_NOT_VERIFIED")
        void 실패_이메일인증미완료() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            given(userMapper.existsByPhone(anyString())).willReturn(0);
            given(tokenStore.get("email:verified:" + EMAIL)).willReturn(null);

            assertThatThrownBy(() -> authService.signup(signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));
        }

        @Test
        @DisplayName("[실패] 필수 약관 미동의 → REQUIRED_TERMS_NOT_AGREED")
        void 실패_필수약관미동의() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            given(userMapper.existsByPhone(anyString())).willReturn(0);
            given(tokenStore.get("email:verified:" + EMAIL)).willReturn("Y");
            given(termsMapper.findByPackageType("SIGNUP")).willReturn(twoTerms());

            // 선택 약관(id=2)만 동의, 필수 약관(id=1) 누락
            assertThatThrownBy(() -> authService.signup(signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(2L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REQUIRED_TERMS_NOT_AGREED));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-00 | 이메일 인증코드 발송
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증코드 발송")
    class SendVerifyCode {

        @Test
        @DisplayName("[정상] 미가입 이메일 → tokenStore.set + sendVerificationEmail 호출")
        void 정상_인증코드발송() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);

            authService.sendVerifyCode(sendCodeReq(EMAIL));

            then(tokenStore).should().set(startsWith("email:verify:"), anyString(), anyLong());
            then(emailService).should().sendVerificationEmail(eq(EMAIL), anyString());
        }

        @Test
        @DisplayName("[실패] 이미 가입된 이메일 → DUPLICATE_EMAIL")
        void 실패_이메일중복() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(1);

            assertThatThrownBy(() -> authService.sendVerifyCode(sendCodeReq(EMAIL)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_EMAIL));

            then(tokenStore).should(never()).set(any(), any(), anyLong());
            then(emailService).should(never()).sendVerificationEmail(any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-02 | 이메일 인증코드 확인
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증코드 확인")
    class VerifyEmail {

        @Test
        @DisplayName("[정상] 코드 일치 → 코드 삭제 + 인증완료 플래그 저장")
        void 정상_인증완료() {
            given(tokenStore.get("email:verify:" + EMAIL)).willReturn("ABCDEF");

            authService.verifyEmail(emailVerifyReq(EMAIL, "ABCDEF"));

            then(tokenStore).should().delete("email:verify:" + EMAIL);
            then(tokenStore).should().set(eq("email:verified:" + EMAIL), eq("Y"), anyLong());
        }

        @Test
        @DisplayName("[실패] 코드 불일치 또는 만료(null) → VERIFY_TOKEN_INVALID")
        void 실패_코드불일치또는만료() {
            given(tokenStore.get("email:verify:" + EMAIL)).willReturn(null);

            assertThatThrownBy(() -> authService.verifyEmail(emailVerifyReq(EMAIL, "WRONG")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.VERIFY_TOKEN_INVALID));

            then(tokenStore).should(never()).delete(any());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-22 | 비밀번호 재설정 링크 요청 (findPassword)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정 링크 요청")
    class FindPassword {

        @Test
        @DisplayName("[정상] 이름·이메일 일치 → tokenStore.set + sendPasswordResetEmail 호출")
        void 정상_링크발송() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(activeUser()));

            authService.findPassword(findPwReq(EMAIL, NAME));

            then(tokenStore).should().set(startsWith("pw:reset:"), eq(EMAIL), anyLong());
            then(emailService).should().sendPasswordResetEmail(eq(EMAIL), anyString());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 이메일 → USER_NOT_FOUND")
        void 실패_이메일없음() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.findPassword(findPwReq(EMAIL, NAME)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 이름 불일치 → USER_NOT_FOUND")
        void 실패_이름불일치() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(activeUser()));

            assertThatThrownBy(() -> authService.findPassword(findPwReq(EMAIL, "다른이름")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [기존] F-23 | 비밀번호 재설정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정")
    class ResetPassword {

        @Test
        @DisplayName("[정상] 정상 재설정 완료 → 암호화 수정, 세션 파기, 토큰 스토어 삭제")
        void 정상_재설정완료() {
            given(tokenStore.get("pw:reset:" + TOKEN)).willReturn(EMAIL);
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.encode(NEW_PW)).willReturn("$2a$10$newHash");

            authService.resetPassword(resetPwReq(TOKEN, NEW_PW, NEW_PW));

            then(userMapper).should().updatePassword(eq(USER_ID), eq("$2a$10$newHash"), any());
            then(userSessionMapper).should().revokeAllByUserId(USER_ID);
            then(tokenStore).should().delete("pw:reset:" + TOKEN);
        }

        @Test
        @DisplayName("[실패] 새 비밀번호 확인 불일치 → PASSWORD_CONFIRM_MISMATCH")
        void 실패_비밀번호확인불일치() {
            assertThatThrownBy(() -> authService.resetPassword(resetPwReq(TOKEN, NEW_PW, "Different!!")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH));

            then(tokenStore).should(never()).get(any());
        }

        @Test
        @DisplayName("[실패] 만료·없는 토큰(null) → VERIFY_TOKEN_INVALID")
        void 실패_토큰만료() {
            given(tokenStore.get("pw:reset:" + TOKEN)).willReturn(null);

            assertThatThrownBy(() -> authService.resetPassword(resetPwReq(TOKEN, NEW_PW, NEW_PW)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.VERIFY_TOKEN_INVALID));
        }
    }
}