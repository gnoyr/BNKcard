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
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.application.service.CddService;
import com.bnk.domain.auth.dto.request.EmailVerifyRequest;
import com.bnk.domain.auth.dto.request.FindIdRequest;
import com.bnk.domain.auth.dto.request.FindPasswordRequest;
import com.bnk.domain.auth.dto.request.LoginRequest;
import com.bnk.domain.auth.dto.request.ResetPasswordRequest;
import com.bnk.domain.auth.dto.request.SendVerifyCodeRequest;
import com.bnk.domain.auth.dto.request.SignupRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.dto.response.FindIdResponse;
import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.auth.model.UserSession;
import com.bnk.domain.ipauth.service.IpTrustService;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
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
 * AuthService 단위 테스트 (SonarQube 커버리지 대상)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

	// ── Mock ─────────────────────────────────────────────────────────
	@Mock
	private UserMapper userMapper;
	@Mock
	private AdminUserMapper adminUserMapper;
	@Mock
	private UserSessionMapper userSessionMapper;
	@Mock
	private TermsMapper termsMapper;
	@Mock
	private UserTermsAgreementMapper userTermsAgreementMapper;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private CookieUtil cookieUtil;
	@Mock
	private TokenStore tokenStore;
	@Mock
	private EmailService mailService;
	@Mock
	private CiValueGenerator ciValueGenerator;
	@Mock
	private CddService cddService;
	@Mock
	private TokenSecurityService tokenSecurityService;
	@Mock
	private AuditLogger auditLogger;
	@Mock
	private IpTrustService ipTrustService;
	@Mock
	private Clock clock;
	private static final LocalDateTime FIXED_FUTURE = LocalDateTime.of(2099, Month.DECEMBER, 31, 0, 0);
	private static final LocalDateTime FIXED_PAST = LocalDateTime.of(2024, Month.JANUARY, 14, 0, 0); // -1일

	@InjectMocks
	private AuthService authService;

	// ── 공통 상수 ────────────────────────────────────────────────────
	private static final Long USER_ID = 1L;
	private static final String EMAIL = "test@bnk.co.kr";
	private static final String NAME = "홍길동";
	private static final String PHONE = "010-1234-5678";
	private static final String PASSWORD = "Password123!";
	private static final String ENC_PW = "$2a$10$encodedPasswordHashValue";
	private static final String TOKEN = "mock-uuid-token";
	private static final String NEW_PW = "NewSecure123!";

	// ── Fixture ──────────────────────────────────────────────────────

	private User activeUser() {
		User u = new User();
		ReflectionTestUtils.setField(u, "userId", USER_ID);
		ReflectionTestUtils.setField(u, "email", EMAIL);
		ReflectionTestUtils.setField(u, "passwordHash", ENC_PW);
		ReflectionTestUtils.setField(u, "name", NAME);
		ReflectionTestUtils.setField(u, "phone", PHONE);
		ReflectionTestUtils.setField(u, "statusCode", "ACTIVE");
		ReflectionTestUtils.setField(u, "loginFailCount", 0);
		ReflectionTestUtils.setField(u, "deletedYn", "N");
		return u;
	}

	private User userWithFourFails() {
		User u = activeUser();
		ReflectionTestUtils.setField(u, "loginFailCount", 4);
		return u;
	}

	private User userWithStatus(String status) {
		User u = activeUser();
		ReflectionTestUtils.setField(u, "statusCode", status);
		return u;
	}

	private User deletedUser() {
		User u = activeUser();
		ReflectionTestUtils.setField(u, "statusCode", "WITHDRAWN");
		return u;
	}

	private HttpServletRequest mockHttpReq() {
		HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);
		given(req.getRemoteAddr()).willReturn("127.0.0.1");
		given(req.getHeader("User-Agent")).willReturn("JUnit-Test");
		return req;
	}

	// ── Request DTO 빌더 헬퍼 ────────────────────────────────────────

	private LoginRequest loginReq(String email, String pw) {
		LoginRequest req = new LoginRequest();
		ReflectionTestUtils.setField(req, "email", email);
		ReflectionTestUtils.setField(req, "password", pw);
		return req;
	}

	private SignupRequest signupReq(String email, String pw, String name, String phone, List<Long> termIds) {
		SignupRequest req = new SignupRequest();
		ReflectionTestUtils.setField(req, "email", email);
		ReflectionTestUtils.setField(req, "password", pw);
		ReflectionTestUtils.setField(req, "name", name);
		ReflectionTestUtils.setField(req, "phone", phone);
		ReflectionTestUtils.setField(req, "birthDate", "19950525");
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
		ReflectionTestUtils.setField(req, "code", code);
		return req;
	}

	private FindIdRequest findIdReq(String name, String phone) {
		FindIdRequest req = new FindIdRequest();
		ReflectionTestUtils.setField(req, "name", name);
		ReflectionTestUtils.setField(req, "phone", phone);
		return req;
	}

	private FindPasswordRequest findPwReq(String email, String name) {
		FindPasswordRequest req = new FindPasswordRequest();
		ReflectionTestUtils.setField(req, "email", email);
		ReflectionTestUtils.setField(req, "name", name);
		return req;
	}

	private ResetPasswordRequest resetPwReq(String token, String pw, String confirm) {
		ResetPasswordRequest req = new ResetPasswordRequest();
		ReflectionTestUtils.setField(req, "token", token);
		ReflectionTestUtils.setField(req, "newPassword", pw);
		ReflectionTestUtils.setField(req, "newPasswordConfirm", confirm);
		return req;
	}

	// ── 로그인 성공 공통 stub ────────────────────────────────────────

	@BeforeEach
	void setupLoginStubs() {
		given(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).willReturn("acc-token");
		given(jwtTokenProvider.generateRefreshToken(USER_ID)).willReturn("ref-token");
		given(jwtTokenProvider.getAccessExpirationSec()).willReturn(7200L);
		given(jwtTokenProvider.getRefreshExpirationSec()).willReturn(604800L);
		given(cookieUtil.createAccessCookie("acc-token", 7200L))
				.willReturn(ResponseCookie.from("access_token", "acc-token").build());
		given(cookieUtil.createRefreshCookie("ref-token", 604800L))
				.willReturn(ResponseCookie.from("refresh_token", "ref-token").build());
		Instant fixedInstant = Instant.parse("2025-01-01T00:00:00Z");
		given(clock.instant()).willReturn(fixedInstant);
		given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
	}

	// ════════════════════════════════════════════════════════════════
	// TC-01 | 이메일 인증코드 발송
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("이메일 인증코드 발송 [sendVerifyCode]")
	class SendVerifyCode {

		@Test
		@DisplayName("[정상] 미가입 이메일 → tokenStore.set + sendVerificationEmail 호출")
		void 정상_인증코드발송() {
			given(userMapper.existsByEmail(EMAIL)).willReturn(0);

			authService.sendVerifyCode(sendCodeReq(EMAIL));

			then(tokenStore).should().set(startsWith("email:verify:"), anyString(), anyLong());
			then(mailService).should().sendVerificationEmail(eq(EMAIL), anyString());
		}

		@Test
		@DisplayName("[실패] 이미 가입된 이메일 → DUPLICATE_EMAIL + tokenStore·mailService 미호출")
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
	// TC-02 | 이메일 인증코드 확인
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("이메일 인증코드 확인 [verifyEmail]")
	class VerifyEmail {

		@Test
		@DisplayName("[정상] 코드 일치 → 코드 삭제 + 인증완료 플래그(Y) 저장")
		void 정상_인증완료() {
			given(tokenStore.get("email:verify:" + EMAIL)).willReturn("ABCDEF");

			authService.verifyEmail(emailVerifyReq(EMAIL, "ABCDEF"));

			then(tokenStore).should().delete("email:verify:" + EMAIL);
			then(tokenStore).should().set(eq("email:verified:" + EMAIL), eq("Y"), anyLong());
		}

		@Test
		@DisplayName("[실패] 코드 불일치 → VERIFY_TOKEN_INVALID")
		void 실패_코드불일치() {
			given(tokenStore.get("email:verify:" + EMAIL)).willReturn("ABCDEF");

			assertThatThrownBy(() -> authService.verifyEmail(emailVerifyReq(EMAIL, "ZZZZZZ")))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.VERIFY_TOKEN_INVALID));

			then(tokenStore).should(never()).delete(any());
		}

		@Test
		@DisplayName("[실패] TTL 만료(null) → VERIFY_TOKEN_INVALID")
		void 실패_TTL만료() {
			given(tokenStore.get("email:verify:" + EMAIL)).willReturn(null);

			assertThatThrownBy(() -> authService.verifyEmail(emailVerifyReq(EMAIL, "ABCDEF")))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.VERIFY_TOKEN_INVALID));
		}
	}

	// ════════════════════════════════════════════════════════════════
	// TC-03 | 회원가입
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("회원가입 [signup]")
	class Signup {

		@BeforeEach
		void stubSignupCommon() {
			given(userMapper.findAllPhones()).willReturn(Collections.emptyList());
			given(tokenStore.get("email:verified:" + EMAIL)).willReturn("Y");
			given(termsMapper.findByPackageType("SIGNUP")).willReturn(Collections.emptyList());
			given(ciValueGenerator.generate(anyString(), anyString(), anyString())).willReturn("mock-ci")
					.willReturn("mock-ci");
			willDoNothing().given(cddService).checkWatchlist(any(), any(), any());
			willDoNothing().given(cddService).initializeCdd(anyLong());
			given(userMapper.insertUser(any())).willAnswer(inv -> {
				User u = inv.getArgument(0);
				ReflectionTestUtils.setField(u, "userId", USER_ID);
				return 1;
			});
			given(passwordEncoder.encode(PASSWORD)).willReturn(ENC_PW);
		}

		@Test
		@DisplayName("[정상] 이메일 인증 완료 + 미중복 → userId 반환 + insertUser 호출")
		void 정상_회원가입() {
			given(userMapper.existsByEmail(EMAIL)).willReturn(0);

			Long result = authService.signup(signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L)));

			assertThat(result).isEqualTo(USER_ID);
			then(userMapper).should().insertUser(any(User.class));
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
		@DisplayName("[실패] 이메일 인증 미완료(플래그 없음) → EMAIL_NOT_VERIFIED")
		void 실패_이메일인증미완료() {
			given(userMapper.existsByEmail(EMAIL)).willReturn(0);
			given(tokenStore.get("email:verified:" + EMAIL)).willReturn(null);

			assertThatThrownBy(() -> authService.signup(signupReq(EMAIL, PASSWORD, NAME, PHONE, List.of(1L))))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));
		}
	}

	// ════════════════════════════════════════════════════════════════
	// TC-04 | 로그인
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("로그인 [login]")
	class Login {

		@Test
		@DisplayName("[정상] 정상 계정 + 올바른 비밀번호 → AccessCookie + RefreshCookie 반환")
		void 정상_로그인() {
			given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(activeUser()));
			given(passwordEncoder.matches(PASSWORD, ENC_PW)).willReturn(true);
			given(userSessionMapper.insertSession(any())).willReturn(1);

			AuthTokenResult result = authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq());

			assertThat(result).isNotNull();
			assertThat(result.getAccessCookie()).isNotNull();
			assertThat(result.getRefreshCookie()).isNotNull();
		}

		@Test
		@DisplayName("[실패] 존재하지 않는 이메일 → INVALID_CREDENTIALS")
		void 실패_이메일없음() {
			given(userMapper.findByEmail(EMAIL)).willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.INVALID_CREDENTIALS));
		}

		@Test
		@DisplayName("[실패] 탈퇴 회원(deletedYn=Y) → ACCOUNT_WITHDRAWN")
		void 실패_탈퇴회원() {
			given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(deletedUser()));

			assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN));
		}

		@Test
		@DisplayName("[실패] 비밀번호 불일치 → INVALID_CREDENTIALS + incrementLoginFailCount 호출")
		void 실패_비밀번호불일치() {
			given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(activeUser()));
			given(passwordEncoder.matches(PASSWORD, ENC_PW)).willReturn(false);

			assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.INVALID_CREDENTIALS));

			then(userMapper).should().incrementLoginFailCount(USER_ID);
		}

		@Test
		@DisplayName("[실패] 5회 실패 → updateLockedUntil 호출(계정 잠금)")
		void 실패_5회오류_계정잠금() {
			given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithFourFails()));
			given(passwordEncoder.matches(PASSWORD, ENC_PW)).willReturn(false);

			assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
					.isInstanceOf(BusinessException.class);

			then(userMapper).should().incrementLoginFailCount(USER_ID);
			then(userMapper).should().updateLockedUntil(eq(USER_ID), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("[실패] SUSPENDED 계정 → ACCOUNT_SUSPENDED")
		void 실패_정지계정() {
			given(userMapper.findByEmail(EMAIL)).willReturn(Optional.of(userWithStatus("SUSPENDED")));

			assertThatThrownBy(() -> authService.login(loginReq(EMAIL, PASSWORD), mockHttpReq()))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.ACCOUNT_SUSPENDED));
		}
	}

	// ════════════════════════════════════════════════════════════════
	// TC-05 | 로그아웃
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("로그아웃 [logout]")
	class Logout {

		@Test
		@DisplayName("[정상] revokeAllByUserId 호출 확인")
		void 정상_로그아웃() {
			given(userSessionMapper.revokeAllByUserId(USER_ID)).willReturn(1);

			authService.logout(USER_ID);

			then(userSessionMapper).should().revokeAllByUserId(USER_ID);
		}
	}

	// ════════════════════════════════════════════════════════════════
	// TC-06 | Access Token 재발급
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("Access Token 재발급 [refresh]")
	class Refresh {

		@Test
		@DisplayName("[정상] 유효한 세션 → 새 AccessCookie 반환")
		void 정상_토큰재발급() {
			UserSession session = new UserSession();
			ReflectionTestUtils.setField(session, "userId", USER_ID);
			ReflectionTestUtils.setField(session, "expiresAt", FIXED_FUTURE); // 2024-01-22

			given(userSessionMapper.findByRefreshToken("valid-rt")).willReturn(Optional.of(session));
			given(jwtTokenProvider.generateAccessToken(USER_ID, "ROLE_USER")).willReturn("new-access-token");
			given(jwtTokenProvider.getAccessExpirationSec()).willReturn(1800L);
			given(cookieUtil.createAccessCookie("new-access-token", 1800L))
					.willReturn(ResponseCookie.from("access_token", "new-access-token").path("/").build());

			ResponseCookie cookie = authService.refresh("valid-rt");

			assertThat(cookie).isNotNull();
		}

		@Test
		@DisplayName("[실패] 세션 없음 → REFRESH_TOKEN_INVALID")
		void 실패_세션없음() {
			given(userSessionMapper.findByRefreshToken("invalid-rt")).willReturn(Optional.empty());

			assertThatThrownBy(() -> authService.refresh("invalid-rt")).isInstanceOf(BusinessException.class).satisfies(
					e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
		}

		@Test
		@DisplayName("[실패] 세션 TTL 만료 → REFRESH_TOKEN_INVALID")
		void 실패_세션만료() {
			UserSession expired = new UserSession();
			ReflectionTestUtils.setField(expired, "userId", USER_ID);
			ReflectionTestUtils.setField(expired, "expiresAt", FIXED_PAST); // 2024-01-14

			given(userSessionMapper.findByRefreshToken("expired-rt")).willReturn(Optional.of(expired));

			assertThatThrownBy(() -> authService.refresh("expired-rt")).isInstanceOf(BusinessException.class).satisfies(
					e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
		}
	}

	// ════════════════════════════════════════════════════════════════
	// TC-07 | 아이디 찾기
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("아이디 찾기 [findId]")
	class FindId {

		@Test
		@DisplayName("[정상] 이름+전화번호 일치 → maskedEmail 반환")
		void 정상_아이디찾기() {
			given(userMapper.findByName(NAME)).willReturn(List.of(activeUser()));

			FindIdResponse response = authService.findId(findIdReq(NAME, PHONE));

			assertThat(response).isNotNull();
			assertThat(response.getMaskedEmail()).isNotBlank();
		}

		@Test
		@DisplayName("[실패] 이름으로 회원 없음 → USER_NOT_FOUND")
		void 실패_이름없음() {
			given(userMapper.findByName("없는사람")).willReturn(Collections.emptyList());

			assertThatThrownBy(() -> authService.findId(findIdReq("없는사람", PHONE))).isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.USER_NOT_FOUND));
		}

		@Test
		@DisplayName("[실패] 이름 일치하지만 전화번호 불일치 → USER_NOT_FOUND")
		void 실패_전화번호불일치() {
			given(userMapper.findByName(NAME)).willReturn(List.of(activeUser()));

			assertThatThrownBy(() -> authService.findId(findIdReq(NAME, "010-9999-9999")))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.USER_NOT_FOUND));
		}
	}

	// ════════════════════════════════════════════════════════════════
	// TC-08 | 비밀번호 재설정 링크 발송
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("비밀번호 재설정 링크 발송 [findPassword]")
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

			then(tokenStore).should(never()).set(any(), any(), anyLong());
		}
	}

	// ════════════════════════════════════════════════════════════════
	// TC-09 | 비밀번호 재설정
	// ════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("비밀번호 재설정 [resetPassword]")
	class ResetPassword {

		@Test
		@DisplayName("[정상] 유효 토큰 + 비밀번호 일치 → updatePassword 호출 + 토큰 삭제")
		void 정상_비밀번호재설정() {
			given(tokenStore.get("pw:reset:" + TOKEN)).willReturn(String.valueOf(USER_ID));
			given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
			given(passwordEncoder.encode(NEW_PW)).willReturn("$2a$10$newHash");

			authService.resetPassword(resetPwReq(TOKEN, NEW_PW, NEW_PW));

			then(userMapper).should().updatePassword(eq(USER_ID), anyString(), any(java.time.LocalDateTime.class));
			then(tokenStore).should().delete("pw:reset:" + TOKEN);
		}

		@Test
		@DisplayName("[실패] 비밀번호 확인 불일치 → PASSWORD_CONFIRM_MISMATCH (tokenStore 미조회)")
		void 실패_비밀번호확인불일치() {
			assertThatThrownBy(() -> authService.resetPassword(resetPwReq(TOKEN, NEW_PW, "WrongConfirm!")))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH));

			then(tokenStore).should(never()).get(any());
		}

		@Test
		@DisplayName("[실패] 만료·없는 토큰(null) → VERIFY_TOKEN_INVALID")
		void 실패_토큰없음() {
			given(tokenStore.get("pw:reset:expired")).willReturn(null);

			assertThatThrownBy(() -> authService.resetPassword(resetPwReq("expired", NEW_PW, NEW_PW)))
					.isInstanceOf(BusinessException.class)
					.satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
							.isEqualTo(ErrorCode.VERIFY_TOKEN_INVALID));
		}
	}
}