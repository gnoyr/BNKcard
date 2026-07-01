package com.bnk.domain.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bnk.domain.auth.dto.request.*;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.dto.response.FindIdResponse;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.exception.ErrorResponse;
import com.bnk.global.exception.GlobalExceptionHandler;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.audit.AuditLogger;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.MissingRequestCookieException;

/**
 * AuthController 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 설계 원칙 ────────────────────────────────────────────────────────
 * · standaloneSetup — Spring Context 없는 경량 테스트
 * · GlobalExceptionHandler(AuditLogger) 실제 등록 → ErrorCode JSON 검증
 * · X-No-Auth 헤더 → CustomUserDetails null → 로그아웃 시 authService.logout 미호출
 * · login(): deleteLegacyRefreshCookie() stub 필수 (없으면 NPE)
 * · refresh(): @CookieValue(required=false) → 쿠키 없으면 null → 컨트롤러 직접 401
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    private MockMvc mvc;

    @InjectMocks private AuthController authController;
    @Mock         private AuthService   authService;
    @Mock         private CookieUtil    cookieUtil;
    @Mock         private AuditLogger   auditLogger;

    private CustomUserDetails mockUserDetails;

    private static final String TOKEN  = "ABCDEF123456";
    private static final String NEW_PW = "NewSecure123!";

    /** standalone 환경에서 MissingRequestCookieException 처리 */
    @RestControllerAdvice
    static class TestCookieExceptionHandler {
        @ExceptionHandler(MissingRequestCookieException.class)
        public ResponseEntity<ErrorResponse> handle(MissingRequestCookieException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                            "쿠키 누락: " + ex.getCookieName()));
        }
    }

    @BeforeEach
    void setup() {
        mockUserDetails = Mockito.mock(CustomUserDetails.class);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter p) {
                return p.hasParameterAnnotation(
                        org.springframework.security.core.annotation.AuthenticationPrincipal.class);
            }
            @Override
            public Object resolveArgument(MethodParameter p, ModelAndViewContainer mvc,
                                          NativeWebRequest req, WebDataBinderFactory binder) {
                return req.getHeader("X-No-Auth") != null ? null : mockUserDetails;
            }
        };

        mvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogger),
                                     new TestCookieExceptionHandler())
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // 이메일 인증코드 발송  POST /api/auth/send-verify-code
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증코드 발송 API [POST /api/auth/send-verify-code]")
    class SendVerifyCodeApi {

        @Test
        @DisplayName("[성공] 미가입 이메일 → 200")
        void 정상_200() throws Exception {
            willDoNothing().given(authService).sendVerifyCode(any(SendVerifyCodeRequest.class));

            mvc.perform(post("/api/auth/send-verify-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"hong@bnk.co.kr\"}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 이미 가입된 이메일 → 409 + code=U002")
        void 실패_이메일중복_409() throws Exception {
            willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
                    .given(authService).sendVerifyCode(any());

            mvc.perform(post("/api/auth/send-verify-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"dup@bnk.co.kr\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("U002"));
        }

        @Test
        @DisplayName("[실패] 이메일 형식 오류 → 400 + code=C001")
        void 실패_이메일형식오류_400() throws Exception {
            mvc.perform(post("/api/auth/send-verify-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"notAnEmail\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 이메일 인증코드 확인  POST /api/auth/verify-email
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증코드 확인 API [POST /api/auth/verify-email]")
    class VerifyEmailApi {

        @Test
        @DisplayName("[성공] 코드 일치 → 200")
        void 정상_200() throws Exception {
            willDoNothing().given(authService).verifyEmail(any(EmailVerifyRequest.class));

            mvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"hong@bnk.co.kr\",\"code\":\"ABCDEF\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 코드 불일치·만료 → 400 + code=U008")
        void 실패_코드불일치_400() throws Exception {
            willThrow(new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID))
                    .given(authService).verifyEmail(any());

            mvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"hong@bnk.co.kr\",\"code\":\"ZZZZZZ\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U008"));
        }

        @Test
        @DisplayName("[실패] code 필드 누락 → 400 + code=C001")
        void 실패_코드누락_400() throws Exception {
            mvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"hong@bnk.co.kr\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 회원가입  POST /api/auth/signup
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("회원가입 API [POST /api/auth/signup]")
    class SignupApi {

    	private static final String VALID_JSON =
    		    "{\"email\":\"test@bnk.com\",\"password\":\"Abcd123!\"," +
    		    "\"name\":\"홍길동\",\"phone\":\"01012345678\"," +
    		    "\"residentFront\":\"950525\",\"genderCode\":\"1\"," +
    		    "\"address\":\"부산광역시 해운대구 테스트로 1\"," +
    		    "\"birthDate\":\"19950525\"," +
    		    "\"agreedTermsIds\":[1,2]}";

        @Test
        @DisplayName("[성공] 정상 회원가입 → 201 Created")
        void 정상_201() throws Exception {
            given(authService.signup(any(SignupRequest.class))).willReturn(1L);
            willDoNothing().given(authService).registerInitialDevice(anyLong(), any(), any(), any(), any());

            mvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("[실패] 이메일 중복 → 409 + code=U002")
        void 실패_중복이메일_409() throws Exception {
            given(authService.signup(any(SignupRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

            mvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("U002"));
        }

        @Test
        @DisplayName("[실패] 이메일 인증 미완료 → 403 + code=U007")
        void 실패_이메일인증미완료_403() throws Exception {
            given(authService.signup(any(SignupRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));

            mvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("U007"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 로그인  POST /api/auth/login
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("로그인 API [POST /api/auth/login]")
    class LoginApi {

        private static final String LOGIN_JSON =
                "{\"email\":\"hong@bnk.co.kr\",\"password\":\"Abcd123!\"}";

        @Test
        @DisplayName("[성공] 올바른 계정 정보 → 200")
        void 정상_200() throws Exception {
            AuthTokenResult tokenResult = AuthTokenResult.builder()
                    .accessCookie(ResponseCookie.from("access_token",  "mock-at").path("/").build())
                    .refreshCookie(ResponseCookie.from("refresh_token", "mock-rt").path("/").build())
                    .build();

            given(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                    .willReturn(tokenResult);
            // deleteLegacyRefreshCookie stub 필수 — 없으면 NPE → 500
            given(cookieUtil.deleteLegacyRefreshCookie())
                    .willReturn(ResponseCookie.from("refresh_token", "").maxAge(0).path("/").build());

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 이메일 없음 → 404 + code=U001")
        void 실패_이메일없음_404() throws Exception {
            given(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }

        @Test
        @DisplayName("[실패] 비밀번호 불일치 → 400 + code=U003")
        void 실패_비밀번호불일치_400() throws Exception {
            given(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD));

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U003"));
        }

        @Test
        @DisplayName("[실패] 계정 잠금 → 423 + code=U004")
        void 실패_계정잠금_423() throws Exception {
            given(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED));

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.code").value("U004"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Access Token 재발급  POST /api/auth/refresh
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Access Token 재발급 API [POST /api/auth/refresh]")
    class RefreshApi {

        @Test
        @DisplayName("[성공] 유효한 refresh_token 쿠키 → 200")
        void 정상_200() throws Exception {
            given(authService.refresh(anyString()))
                    .willReturn(ResponseCookie.from("access_token", "new-at").path("/").build());

            mvc.perform(post("/api/auth/refresh")
                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-rt")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] refresh_token 쿠키 없음 → 401 (컨트롤러 직접 반환)")
        void 실패_쿠키없음_401() throws Exception {
            // @CookieValue(required=false) → null → 컨트롤러 내부 401 직접 반환
            mvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[실패] 만료된 refresh_token → REFRESH_TOKEN_INVALID")
        void 실패_만료토큰() throws Exception {
            given(authService.refresh("expired-rt"))
                    .willThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

            mvc.perform(post("/api/auth/refresh")
                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", "expired-rt")))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 로그아웃  POST /api/auth/logout
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("로그아웃 API [POST /api/auth/logout]")
    class LogoutApi {

        @BeforeEach
        void stubCookies() {
            given(cookieUtil.deleteAccessCookie())
                    .willReturn(ResponseCookie.from("access_token",  "").maxAge(0).build());
            given(cookieUtil.deleteRefreshCookie())
                    .willReturn(ResponseCookie.from("refresh_token", "").maxAge(0).build());
        }

        @Test
        @DisplayName("[성공] 로그인 상태 → authService.logout(1L) 호출 + 204")
        void 정상_204() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);

            mvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent());

            then(authService).should().logout(1L);
        }

        @Test
        @DisplayName("[성공] 비로그인 상태 → authService.logout 미호출 + 204")
        void 정상_비로그인_204() throws Exception {
            mvc.perform(post("/api/auth/logout").header("X-No-Auth", "true"))
                    .andExpect(status().isNoContent());

            then(authService).should(never()).logout(anyLong());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 아이디 찾기  POST /api/auth/find-id
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("아이디 찾기 API [POST /api/auth/find-id]")
    class FindIdApi {

        @Test
        @DisplayName("[성공] 이름+전화번호 일치 → 200 + maskedEmail")
        void 정상_200() throws Exception {
            given(authService.findId(any(FindIdRequest.class)))
                    .willReturn(FindIdResponse.builder()
                            .maskedEmail("ho**@bnk.co.kr")
                            .message("이메일 조회 완료")
                            .build());

            mvc.perform(post("/api/auth/find-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"홍길동\",\"phone\":\"01012345678\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maskedEmail").value("ho**@bnk.co.kr"));
        }

        @Test
        @DisplayName("[실패] 정보 불일치 → 404 + code=U001")
        void 실패_사용자없음_404() throws Exception {
            given(authService.findId(any(FindIdRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            mvc.perform(post("/api/auth/find-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"없는사람\",\"phone\":\"01099999999\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 비밀번호 재설정 링크 요청  POST /api/auth/find-password
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정 링크 요청 API [POST /api/auth/find-password]")
    class FindPasswordApi {

        @Test
        @DisplayName("[성공] 이메일+이름 일치 → 200")
        void 정상_200() throws Exception {
            willDoNothing().given(authService).findPassword(any(FindPasswordRequest.class));

            mvc.perform(post("/api/auth/find-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"hong@bnk.co.kr\",\"name\":\"홍길동\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 이메일+이름 불일치 → 404 + code=U001")
        void 실패_사용자없음_404() throws Exception {
            willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                    .given(authService).findPassword(any());

            mvc.perform(post("/api/auth/find-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"none@bnk.co.kr\",\"name\":\"없는사람\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 비밀번호 재설정  POST /api/auth/reset-password
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정 API [POST /api/auth/reset-password]")
    class ResetPasswordApi {

        @Test
        @DisplayName("[성공] 유효한 토큰 + 비밀번호 → 200")
        void 정상_200() throws Exception {
            willDoNothing().given(authService).resetPassword(any(ResetPasswordRequest.class));

            mvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + TOKEN + "\",\"newPassword\":\"" + NEW_PW +
                             "\",\"newPasswordConfirm\":\"" + NEW_PW + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 비밀번호 확인 불일치 → 400 + code=U009")
        void 실패_비밀번호불일치_400() throws Exception {
            willThrow(new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH))
                    .given(authService).resetPassword(any());

            mvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + TOKEN + "\",\"newPassword\":\"" + NEW_PW +
                             "\",\"newPasswordConfirm\":\"wrong!\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U009"));
        }

        @Test
        @DisplayName("[실패] 만료·없는 토큰 → 400 + code=U008")
        void 실패_토큰만료_400() throws Exception {
            willThrow(new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID))
                    .given(authService).resetPassword(any());

            mvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"expired\",\"newPassword\":\"" + NEW_PW +
                             "\",\"newPasswordConfirm\":\"" + NEW_PW + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U008"));
        }
    }
}