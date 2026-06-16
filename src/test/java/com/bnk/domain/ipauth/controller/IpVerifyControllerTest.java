package com.bnk.domain.ipauth.controller;

import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.ipauth.service.IpTrustService;
import com.bnk.domain.ipauth.service.IpVerifyService;
import com.bnk.global.auth.JwtTokenProvider;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.exception.GlobalExceptionHandler;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.audit.AuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * IpVerifyController 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IpVerifyController 단위 테스트")
class IpVerifyControllerTest {

	@Mock
	IpTrustService ipTrustService;
	@Mock
	IpVerifyService ipVerifyService;
	@Mock
	JwtTokenProvider jwtTokenProvider;
	@Mock
	UserSessionMapper userSessionMapper;
	@Mock
	CookieUtil cookieUtil;
	@Mock
	AuditLogger auditLogger;

	@InjectMocks
	IpVerifyController controller;

	MockMvc mvc;
	ObjectMapper om = new ObjectMapper();

	private static final Long USER_ID = 42L;
	private static final String CHALLENGE_TOKEN = "ip:challenge:42:bbb222hash";
	private static final String PLAIN_IP = "10.0.0.99";

	// [COMPILE-FIX] CI 테스트용 공통 상수 — residentFront/genderCode 대신 실제 CI 생성 인자
	private static final String NAME = "홍길동";
	private static final String BIRTH_DATE = "1990-01-01";
	private static final String PHONE = "010-1234-5678";

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler(auditLogger))
				.build();
	}

	// ─── 이메일 코드 발송 ──────────────────────────────────────────────

	@Nested
	@DisplayName("POST /api/auth/ip-verify/email/send")
	class SendEmailCode {

		@Test
		@DisplayName("[성공] 정상 발송 → 200")
		void 정상발송() throws Exception {
			willDoNothing().given(ipVerifyService).sendEmailVerifyCode(USER_ID, CHALLENGE_TOKEN);

			mvc.perform(post("/api/auth/ip-verify/email/send").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN))))
					.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("[실패] 챌린지 만료 → 400 IP001")
		void 챌린지만료() throws Exception {
			willThrow(new BusinessException(ErrorCode.IP_CHALLENGE_EXPIRED)).given(ipVerifyService)
					.sendEmailVerifyCode(USER_ID, CHALLENGE_TOKEN);

			mvc.perform(post("/api/auth/ip-verify/email/send").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN))))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IP001"));
		}
	}

	// ─── 이메일 코드 확인 ──────────────────────────────────────────────

	@Nested
	@DisplayName("POST /api/auth/ip-verify/email/confirm")
	class ConfirmEmailCode {

		@BeforeEach
		void stubCookies() {
			lenient().when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("mock-at");
			lenient().when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("mock-rt");
			lenient().when(jwtTokenProvider.getAccessExpirationSec()).thenReturn(7200L);
			lenient().when(jwtTokenProvider.getRefreshExpirationSec()).thenReturn(604800L);
			lenient().when(cookieUtil.createAccessCookie(anyString(), anyLong()))
					.thenReturn(ResponseCookie.from("access_token", "mock-at").build());
			lenient().when(cookieUtil.createRefreshCookie(anyString(), anyLong()))
					.thenReturn(ResponseCookie.from("refresh_token", "mock-rt").build());
			lenient().when(userSessionMapper.insertSession(any())).thenReturn(1);
		}

		@Test
		@DisplayName("[성공] 이메일 인증 완료 → 200")
		void 이메일인증성공() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);
			willDoNothing().given(ipVerifyService).verifyEmailCode(USER_ID, "A3F9ZK");
			willDoNothing().given(ipTrustService).approvePendingIp(USER_ID, PLAIN_IP, "EMAIL_VERIFY", "집 컴퓨터");

			mvc.perform(post("/api/auth/ip-verify/email/confirm").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN, "code",
							"A3F9ZK", "nickname", "집 컴퓨터"))))
					.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("[실패] 코드 불일치 → 400 IP007")
		void 코드불일치() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);
			willThrow(new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID)).given(ipVerifyService)
					.verifyEmailCode(USER_ID, "WRONG1");

			mvc.perform(post("/api/auth/ip-verify/email/confirm").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(
							Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN, "code", "WRONG1"))))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IP007"));
		}
	}

	// ─── CI 인증 ──────────────────────────────────────────────────────

	@Nested
	@DisplayName("POST /api/auth/ip-verify/ci")
	class CiVerify {

		@BeforeEach
		void stubCookies() {
			lenient().when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("mock-at");
			lenient().when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("mock-rt");
			lenient().when(jwtTokenProvider.getAccessExpirationSec()).thenReturn(7200L);
			lenient().when(jwtTokenProvider.getRefreshExpirationSec()).thenReturn(604800L);
			lenient().when(cookieUtil.createAccessCookie(anyString(), anyLong()))
					.thenReturn(ResponseCookie.from("access_token", "mock-at").build());
			lenient().when(cookieUtil.createRefreshCookie(anyString(), anyLong()))
					.thenReturn(ResponseCookie.from("refresh_token", "mock-rt").build());
			lenient().when(userSessionMapper.insertSession(any())).thenReturn(1);
		}

		/**
		 * [성공] CI 인증 완료 → 200
		 *
		 * [COMPILE-FIX] stub 수정 수정 전: .verifyCi(eq(USER_ID), eq("900101"), eq("1"),
		 * any()) → 4개 인자, 서비스는 5개 파라미터 → 컴파일 에러 수정 후: .verifyCi(eq(USER_ID), eq(NAME),
		 * eq(BIRTH_DATE), eq(PHONE), any(IpTrustService.class)) → 5개 인자, 서비스 시그니처 일치
		 *
		 * 요청 바디도 residentFront/genderCode → name/birthDate/phone 으로 변경
		 */
		@Test
		@DisplayName("[성공] CI 인증 완료 → 200")
		void CI인증성공() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);

			// [COMPILE-FIX] 5개 파라미터로 수정
			willDoNothing().given(ipVerifyService).verifyCi(eq(USER_ID), eq(NAME), eq(BIRTH_DATE), eq(PHONE),
					any(IpTrustService.class));

			willDoNothing().given(ipTrustService).approvePendingIp(USER_ID, PLAIN_IP, "CI_VERIFY", "회사");

			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN,
							// [COMPILE-FIX] residentFront/genderCode → name/birthDate/phone
							"name", NAME, "birthDate", BIRTH_DATE, "phone", PHONE, "nickname", "회사"))))
					.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
		}

		/**
		 * [실패] CI 불일치 → 400 IP002
		 *
		 * [COMPILE-FIX] stub 수정 — anyString() 3개 + any(IpTrustService.class)
		 */
		@Test
		@DisplayName("[실패] CI 불일치 → 400 IP002")
		void CI불일치() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);

			// [COMPILE-FIX] 수정 전: anyString() x2 + any() → 수정 후: anyString() x3 + any()
			willThrow(new BusinessException(ErrorCode.CI_MISMATCH)).given(ipVerifyService).verifyCi(eq(USER_ID),
					anyString(), anyString(), anyString(), any(IpTrustService.class));

			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN, "name",
							"김철수", "birthDate", "1980-01-01", "phone", "010-9999-8888"))))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IP002"));
		}

		/**
		 * [실패] 이름 누락 → 400 C001 (Bean Validation)
		 *
		 * [COMPILE-FIX] 기존 "주민번호 7자리 → 400 C001" 테스트를 "필수 필드 누락 → 400 C001" 테스트로 교체
		 * (residentFront @Pattern 검증 제거됨 → 새 DTO 기준 검증 테스트)
		 */
		@Test
		@DisplayName("[실패] 필수 필드(name) 누락 → 400 C001")
		void 필수필드누락() throws Exception {
			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN,
							// name 누락
							"birthDate", BIRTH_DATE, "phone", PHONE))))
					.andExpect(status().isBadRequest());
		}

		/**
		 * [실패] 생년월일 형식 오류 → 400
		 */
		@Test
		@DisplayName("[실패] 생년월일 형식 오류(YYYYMMDD) → 400")
		void 생년월일형식오류() throws Exception {
			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN, "name",
							NAME, "birthDate", "19900101", // YYYY-MM-DD 아님
							"phone", PHONE))))
					.andExpect(status().isBadRequest());
		}

		/**
		 * [실패] CI 3회 실패 잠금 → 429 IP003
		 *
		 * [COMPILE-FIX] stub 수정 — 5개 파라미터 매처
		 */
		@Test
		@DisplayName("[실패] CI 3회 실패 잠금 → 429 IP003")
		void CI잠금() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);

			// [COMPILE-FIX] anyString() x2 + any() → anyString() x3 +
			// any(IpTrustService.class)
			willThrow(new BusinessException(ErrorCode.CI_LOCKED)).given(ipVerifyService).verifyCi(eq(USER_ID),
					anyString(), anyString(), anyString(), any(IpTrustService.class));

			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN, "name",
							"이영희", "birthDate", "1980-01-01", "phone", "010-5555-6666"))))
					.andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("IP003"));
		}
	}
}