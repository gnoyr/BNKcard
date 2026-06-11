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
import java.util.Collections;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.application.service.CddService;
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
import com.bnk.global.util.CiValueGenerator;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.TokenSecurityService;
import com.bnk.global.util.TokenStore;
import com.bnk.global.util.audit.AuditLogger;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AuthService 단위 테스트
 *
 * [수정 이력]
 * - @Mock EmailService emailService → mailService 로 필드명 변경
 *   (AuthService 실제 필드: private final EmailService mailService)
 *   Mockito @InjectMocks 는 타입+필드명으로 주입하므로 이름이 다르면 null 주입 → NPE
 * - @BeforeEach clock 주입 제거
 *   (AuthService에 Clock 필드 없음 — KST_ZONE 고정 사용)
 * - @Mock CiValueGenerator, CddService, TokenSecurityService, AuditLogger 추가
 *   (@RequiredArgsConstructor 생성자에 포함된 의존성 전부 Mock 필요)
 * - @MockitoSettings(strictness = LENIENT) 적용
 *   (stubLoginSuccess() 내 일부 스텁이 특정 테스트에서 사용 안 됨 → UnnecessaryStubbingException 방지)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    // ── Mock ──────────────────────────────────────────────────────
    @Mock private UserMapper                 userMapper;
    @Mock private AdminUserMapper            adminUserMapper;
    @Mock private UserSessionMapper          userSessionMapper;
    @Mock private TermsMapper                termsMapper;
    @Mock private UserTermsAgreementMapper   userTermsAgreementMapper;
    @Mock private PasswordEncoder            passwordEncoder;
    @Mock private JwtTokenProvider           jwtTokenProvider;
    @Mock private CookieUtil                 cookieUtil;
    @Mock private TokenStore                 tokenStore;

    // ▼ 핵심 수정 ①: 필드명을 실제 AuthService와 동일하게 'mailService' 로 지정
    @Mock private EmailService               mailService;

    // ▼ 핵심 수정 ②: 누락된 의존성 Mock 추가
    @Mock private CiValueGenerator           ciValueGenerator;
    @Mock private CddService                 cddService;
    @Mock private TokenSecurityService       tokenSecurityService;
    @Mock private AuditLogger                auditLogger;

    @InjectMocks
    private AuthService authService;

    // ▼ 핵심 수정 ③: @BeforeEach clock 주입 제거
    //   AuthService 에 Clock 필드 없음 — LocalDateTime.now(KST_ZONE) 직접 사용
    //   (이전 코드: ReflectionTestUtils.setField(authService, "clock", fixedClock)
    //    → "clock" 필드 없으면 IllegalArgumentException 발생 → 전 테스트 skip)

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
        ReflectionTestUtils.setField(user, "userId",         USER_ID);
        ReflectionTestUtils.setField(user, "email",          EMAIL);
        ReflectionTestUtils.setField(user, "name",           NAME);
        ReflectionTestUtils.setField(user, "passwordHash",   ENC_PW);
        ReflectionTestUtils.setField(user, "statusCode",     "ACTIVE");
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
    //  F-01 | 이메일 인증코드 발송
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
            // ▼ mailService 로 호출 검증 (emailService 가 아님)
            then(mailService).should().sendVerificationEmail(eq(EMAIL), anyString());
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
            then(mailService).should(never()).sendVerificationEmail(any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  F-02 | 이메일 인증코드 확인
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
    //  F-01 | 회원가입
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("회원가입")
    class Signup {

        /** 필수 약관 2개 포함 Terms 목록 */
        private List<Terms> requiredTerms() {
            Terms t1 = new Terms();
            ReflectionTestUtils.setField(t1, "termsId",     1L);
            ReflectionTestUtils.setField(t1, "requiredYn",  "Y");
            ReflectionTestUtils.setField(t1, "version",     "1.0");
            Terms t2 = new Terms();
            ReflectionTestUtils.setField(t2, "termsId",     2L);
            ReflectionTestUtils.setField(t2, "requiredYn",  "Y");
            ReflectionTestUtils.setField(t2, "version",     "1.0");
            return List.of(t1, t2);
        }

        /** 정상 회원가입 공통 스텁 */
        private void stubSignupSuccess() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            given(userMapper.findAllPhones()).willReturn(Collections.emptyList());
            given(tokenStore.get("email:verified:" + EMAIL)).willReturn("Y");
            given(termsMapper.findByPackageType("SIGNUP")).willReturn(requiredTerms());
            given(ciValueGenerator.generate(anyString(), any(), anyString())).willReturn("mock-ci");
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENC_PW);
        }

        @Test
        @DisplayName("[정상] 정상 입력 → userId 반환 + insertUser 호출")
        void 정상_회원가입성공() {
            stubSignupSuccess();
            Mockito.doAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "userId", USER_ID);
                return 1;
            }).when(userMapper).insertUser(any());

            Long result = authService.signup(
                    signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L, 2L)));

            assertThat(result).isEqualTo(USER_ID);
            then(userMapper).should().insertUser(any());
        }

        @Test
        @DisplayName("[실패] 이메일 중복 → DUPLICATE_EMAIL")
        void 실패_이메일중복() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(1);

            assertThatThrownBy(() -> authService.signup(
                    signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L, 2L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
        }

        @Test
        @DisplayName("[실패] 이메일 인증 미완료 → EMAIL_NOT_VERIFIED")
        void 실패_이메일인증미완료() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            given(userMapper.findAllPhones()).willReturn(Collections.emptyList());
            given(tokenStore.get("email:verified:" + EMAIL)).willReturn(null);

            assertThatThrownBy(() -> authService.signup(
                    signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L, 2L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));
        }

        @Test
        @DisplayName("[실패] 필수 약관 미동의 → REQUIRED_TERMS_NOT_AGREED")
        void 실패_필수약관미동의() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            given(userMapper.findAllPhones()).willReturn(Collections.emptyList());
            given(tokenStore.get("email:verified:" + EMAIL)).willReturn("Y");
            given(termsMapper.findByPackageType("SIGNUP")).willReturn(requiredTerms());

            // 약관 ID 누락 (1L만 동의)
            assertThatThrownBy(() -> authService.signup(
                    signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REQUIRED_TERMS_NOT_AGREED));
        }

        @Test
        @DisplayName("[실패] 전화번호 중복 → DUPLICATE_PHONE")
        void 실패_전화번호중복() {
            given(userMapper.existsByEmail(EMAIL)).willReturn(0);
            // 중복 전화번호 유저 반환
            User dup = new User();
            ReflectionTestUtils.setField(dup, "phone", "010-1234-5678"); // MaskingUtil.formatPhone 결과와 동일하게
            given(userMapper.findAllPhones()).willReturn(List.of(dup));

            assertThatThrownBy(() -> authService.signup(
                    signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L, 2L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_PHONE));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  F-03 | 로그인
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
        void 실패_계정잠금() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(lockedUser()));

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
        }

        @Test
        @DisplayName("[실패] 탈퇴 계정 → ACCOUNT_WITHDRAWN")
        void 실패_탈퇴계정() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithStatus("WITHDRAWN")));

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN));
        }

        @Test
        @DisplayName("[실패] 비밀번호 불일치 → INVALID_PASSWORD + 실패 횟수 증가")
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
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithFourFails()));
            given(passwordEncoder.matches(PASSWORD, ENC_PW)).willReturn(false);

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class);

            then(userMapper).should().incrementLoginFailCount(USER_ID);
            then(userMapper).should().updateLockedUntil(eq(USER_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("[실패] 정지 계정 → ACCOUNT_LOCKED (statusCode=SUSPENDED)")
        void 실패_정지계정() {
            given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithStatus("SUSPENDED")));

            assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  F-05 | 로그아웃
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
    //  F-04 | Access Token 재발급
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Access Token 재발급")
    class Refresh {

        private UserSession validSession() {
            return UserSession.builder()
                    .sessionId(10L)
                    .userId(USER_ID)
                    .refreshToken("valid-refresh")
                    .expiresAt(LocalDateTime.now().plusHours(1))  // KST_ZONE 기준과 무관하게 미래
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

            ResponseCookie cookie = authService.refresh("valid-refresh");

            assertThat(cookie).isNotNull();
            assertThat(cookie.getValue()).isEqualTo("new-acc");
        }

        @Test
        @DisplayName("[실패] 세션 없는(revoked/없는) 토큰 → REFRESH_TOKEN_INVALID")
        void 실패_세션없음() {
            given(userSessionMapper.findByRefreshToken("invalid-rt"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("invalid-rt"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  F-22 | 비밀번호 재설정 링크 요청
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정 링크 요청")
    class FindPassword {

        @Test
        @DisplayName("[정상] 이메일+이름 일치 → tokenStore.set + sendPasswordResetEmail 호출")
        void 정상_링크발송() {
            given(userMapper.findByEmailAndName(EMAIL, NAME)).willReturn(Optional.of(activeUser()));

            authService.findPassword(findPwReq(EMAIL, NAME));

            then(tokenStore).should().set(startsWith("pw:reset:"), eq(String.valueOf(USER_ID)), anyLong());
            then(mailService).should().sendPasswordResetEmail(eq(EMAIL), anyString());
        }

        @Test
        @DisplayName("[실패] 이메일+이름 불일치 → USER_NOT_FOUND")
        void 실패_사용자없음() {
            given(userMapper.findByEmailAndName(EMAIL, NAME)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.findPassword(findPwReq(EMAIL, NAME)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] name 누락 → USER_NOT_FOUND")
        void 실패_이름없음() {
            given(userMapper.findByEmailAndName(EMAIL, null)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.findPassword(findPwReq(EMAIL, null)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  F-23 | 비밀번호 재설정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정")
    class ResetPassword {

        @Test
        @DisplayName("[정상] 유효 토큰 + 비밀번호 일치 → updatePassword 호출")
        void 정상_비밀번호재설정() {
            given(tokenStore.get("pw:reset:" + TOKEN)).willReturn(String.valueOf(USER_ID));
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.encode(NEW_PW)).willReturn("$2a$10$newHash");

            authService.resetPassword(resetPwReq(TOKEN, NEW_PW, NEW_PW));

            then(userMapper).should().updatePassword(eq(USER_ID), anyString(), any());
            then(tokenStore).should().delete("pw:reset:" + TOKEN);
        }

        @Test
        @DisplayName("[실패] 비밀번호 확인 불일치 → PASSWORD_CONFIRM_MISMATCH")
        void 실패_비밀번호확인불일치() {
            assertThatThrownBy(() -> authService.resetPassword(resetPwReq(TOKEN, NEW_PW, "WrongConfirm!")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH));

            then(tokenStore).should(never()).get(any());
        }

        @Test
        @DisplayName("[실패] 만료/없는 토큰 → VERIFY_TOKEN_INVALID")
        void 실패_토큰만료() {
            given(tokenStore.get("pw:reset:" + TOKEN)).willReturn(null);

            assertThatThrownBy(() -> authService.resetPassword(resetPwReq(TOKEN, NEW_PW, NEW_PW)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.VERIFY_TOKEN_INVALID));
        }
    }
}
