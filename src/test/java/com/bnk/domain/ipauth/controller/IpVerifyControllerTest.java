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

	private static final String NAME = "홍길동";
	private static final String RESIDENT_FRONT = "900101";
	private static final String GENDER_CODE = "1";
	private static final String ADDRESS = "부산시 중구";

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
		@DisplayName("[실패] 코드 불일치 → 400 U008")
		void 코드불일치() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);
			
			// ErrorCode.java에 존재하는 VERIFY_TOKEN_INVALID(U008) 코드로 일치화 진행
			willThrow(new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID)).given(ipVerifyService)
					.verifyEmailCode(USER_ID, "WRONG1");

			mvc.perform(post("/api/auth/ip-verify/email/confirm").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(
							Map.of("userId", USER_ID, "challengeToken", CHALLENGE_TOKEN, "code", "WRONG1"))))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("U008"));
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

		@Test
		@DisplayName("[성공] CI 인증 완료 → 200")
		void CI인증성공() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);

			willDoNothing().given(ipVerifyService).verifyCi(
					eq(USER_ID),
					anyString(),
					anyString(),
					anyString(),
					anyString(),
					any(IpTrustService.class)
			);

			willDoNothing().given(ipTrustService).approvePendingIp(USER_ID, PLAIN_IP, "CI_VERIFY", "회사");

			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of(
							"userId", USER_ID, 
							"challengeToken", CHALLENGE_TOKEN,
							"name", NAME, 
							"residentFront", RESIDENT_FRONT, 
							"genderCode", GENDER_CODE, 
							"address", ADDRESS, 
							"nickname", "회사"))))
					.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("[실패] CI 정보 불일치 → 400 IP002")
		void CI불일치() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);

			willThrow(new BusinessException(ErrorCode.CI_MISMATCH)).given(ipVerifyService).verifyCi(
					eq(USER_ID),
					anyString(),
					anyString(),
					anyString(),
					anyString(),
					any(IpTrustService.class)
			);

			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of(
							"userId", USER_ID, 
							"challengeToken", CHALLENGE_TOKEN, 
							"name", "김철수", 
							"residentFront", "800101", 
							"genderCode", "1", 
							"address", "서울시 강남구"))))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IP002"));
		}

		@Test
		@DisplayName("[실패] 필수 필드(name) 누락 → 400")
		void 필수필드누락() throws Exception {
			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of(
							"userId", USER_ID, 
							"challengeToken", CHALLENGE_TOKEN,
							"residentFront", RESIDENT_FRONT, 
							"genderCode", GENDER_CODE, 
							"address", ADDRESS))))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("[실패] 주민번호 앞자리 형식 오류 → 400")
		void 주민번호앞자리형식오류() throws Exception {
			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of(
							"userId", USER_ID, 
							"challengeToken", CHALLENGE_TOKEN, 
							"name", NAME, 
							"residentFront", "19900101", 
							"genderCode", GENDER_CODE, 
							"address", ADDRESS))))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("[실패] CI 3회 실패 잠금 → 429 IP003")
		void CI잠금() throws Exception {
			given(ipTrustService.validateChallengeToken(USER_ID, CHALLENGE_TOKEN)).willReturn(PLAIN_IP);

			willThrow(new BusinessException(ErrorCode.CI_LOCKED)).given(ipVerifyService).verifyCi(
					eq(USER_ID),
					anyString(),
					anyString(),
					anyString(),
					anyString(),
					any(IpTrustService.class)
			);

			mvc.perform(post("/api/auth/ip-verify/ci").contentType(MediaType.APPLICATION_JSON)
					.content(om.writeValueAsString(Map.of(
							"userId", USER_ID, 
							"challengeToken", CHALLENGE_TOKEN, 
							"name", "이영희", 
							"residentFront", "800101", 
							"genderCode", "2", 
							"address", "인천시 부평구"))))
					.andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("IP003"));
		}
	}
}