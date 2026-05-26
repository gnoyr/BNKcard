package com.bnk.domain.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bnk.domain.auth.dto.request.EmailVerifyRequest;
import com.bnk.domain.auth.dto.request.FindIdRequest;
import com.bnk.domain.auth.dto.request.FindPasswordRequest;
import com.bnk.domain.auth.dto.request.LoginRequest;
import com.bnk.domain.auth.dto.request.ResetPasswordRequest;
import com.bnk.domain.auth.dto.request.SendVerifyCodeRequest;
import com.bnk.domain.auth.dto.request.SignupRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.dto.response.FindIdResponse;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.exception.ErrorResponse;
import com.bnk.global.exception.GlobalExceptionHandler;
import com.bnk.global.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mvc;

    @InjectMocks private AuthController authController;
    @Mock         private AuthService   authService;
    @Mock         private CookieUtil    cookieUtil;

    private CustomUserDetails mockUserDetails;

    @RestControllerAdvice
    static class TestCookieExceptionHandler {
        @ExceptionHandler(MissingRequestCookieException.class)
        public ResponseEntity<ErrorResponse> handle(MissingRequestCookieException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                            "필수 쿠키 '" + ex.getCookieName() + "'가 누락되었습니다."));
        }
    }

    @BeforeEach
    void setup() {
        // ✅ getUserId() stub을 @BeforeEach에 두지 않음 → UnnecessaryStubbingException 방지
        //    logout 엔드포인트를 제외한 모든 auth API는 @AuthenticationPrincipal이 없으므로
        //    getUserId()가 호출되지 않는다. stub이 필요한 LogoutApi.정상_204에서만 선언한다.
        mockUserDetails = Mockito.mock(CustomUserDetails.class);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override public boolean supportsParameter(MethodParameter p) {
                return p.hasParameterAnnotation(AuthenticationPrincipal.class);
            }
            @Override public Object resolveArgument(MethodParameter p,
                                                    ModelAndViewContainer mvc,
                                                    NativeWebRequest req,
                                                    WebDataBinderFactory binder) {
                // X-No-Auth 헤더 → null(비로그인) 반환
                return req.getHeader("X-No-Auth") != null ? null : mockUserDetails;
            }
        };

        mvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler(),
                                     new TestCookieExceptionHandler())
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // 회원가입 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("회원가입 API [POST /api/auth/signup]")
    class SignupApi {

        private static final String VALID_JSON =
                "{\"email\":\"test@bnk.com\",\"password\":\"Abcd123!\"," +
                "\"name\":\"홍길동\",\"phone\":\"01012345678\",\"birthDate\":\"19950525\"," +
                "\"agreedTermsIds\":[1,2]}";

        @Test
        @DisplayName("[성공] 정상 회원가입 → 201 Created")
        void 정상_201() throws Exception {
            // [수정] toCreated() → HTTP 201
            given(authService.signup(any(SignupRequest.class))).willReturn(1L);

            mvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("[실패] 중복 이메일 → 409 + code=U002")
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

        @Test
        @DisplayName("[실패] 필수 약관 미동의 → 400 + code=T002")
        void 실패_필수약관미동의_400() throws Exception {
            given(authService.signup(any(SignupRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED));

            mvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("T002"));
        }

        @Test
        @DisplayName("[실패] 중복 전화번호 → 409 + code=U010")
        void 실패_중복전화번호_409() throws Exception {
            given(authService.signup(any(SignupRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.DUPLICATE_PHONE));

            mvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("U010"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 로그인 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("로그인 API [POST /api/auth/login]")
    class LoginApi {

        private static final String LOGIN_JSON =
                "{\"email\":\"test@bnk.com\",\"password\":\"Password123!\"}";

        @Test
        @DisplayName("[성공] 올바른 계정 정보 → Set-Cookie 헤더 포함 200")
        void 정상_200() throws Exception {
            AuthTokenResult tokenResult = AuthTokenResult.builder()
                    .accessCookie(ResponseCookie.from("access_token",  "mock-at").path("/").build())
                    .refreshCookie(ResponseCookie.from("refresh_token", "mock-rt").path("/").build())
                    .build();

            given(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                    .willReturn(tokenResult);

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isOk())
                    // [수정] addHeader()로 Set-Cookie를 두 번 추가하면 header().string()은 첫 번째 값만 읽음.
                    //        stringValues()는 전체 헤더 목록을 Iterable로 받아 hasItem()으로 각각 검증한다.
                    .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                            hasItem(containsString("access_token="))))
                    .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                            hasItem(containsString("refresh_token="))));
        }

        // ── [신규] 로그인 실패 분기 ─────────────────────────────────

        @Test
        @DisplayName("[실패] 존재하지 않는 이메일 → 404 + code=U001")
        void 실패_이메일없음_404() throws Exception {
            given(authService.login(any(), any()))
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
            given(authService.login(any(), any()))
                    .willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD));

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U003"));
        }

        @Test
        @DisplayName("[실패] 계정 잠금 → 423 + code=U004")
        void 실패_계정잠김_423() throws Exception {
            given(authService.login(any(), any()))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED));

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.code").value("U004"));
        }

        @Test
        @DisplayName("[실패] 정지 계정 → 403 + code=U005")
        void 실패_계정정지_403() throws Exception {
            given(authService.login(any(), any()))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_SUSPENDED));

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("U005"));
        }

        @Test
        @DisplayName("[실패] 탈퇴 계정 → 403 + code=U006")
        void 실패_탈퇴계정_403() throws Exception {
            given(authService.login(any(), any()))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN));

            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("U006"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 토큰 재발급 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("토큰 재발급 API [POST /api/auth/refresh]")
    class RefreshApi {

        @Test
        @DisplayName("[성공] 유효한 Refresh Token → 쿠키 갱신 200")
        void 정상_200() throws Exception {
            given(authService.refresh(anyString()))
                    .willReturn(ResponseCookie.from("refresh_token", "new-rt")
                            .path("/api/auth/refresh").build());

            mvc.perform(post("/api/auth/refresh")
                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-rt")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 쿠키에 refresh_token 누락 → 400 + code=C001")
        void 실패_쿠키없음_400() throws Exception {
            mvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"));
        }

        @Test
        @DisplayName("[실패] 만료·무효 Refresh Token → 401 + code=A005")
        void 실패_토큰무효_401() throws Exception {
            willThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID))
                    .given(authService).refresh(anyString());

            mvc.perform(post("/api/auth/refresh")
                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", "invalid")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("A005"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 로그아웃 API
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
            // ✅ 이 테스트만 resolver → mockUserDetails → getUserId() 호출 경로를 탄다.
            //    나머지 auth API는 @AuthenticationPrincipal이 없어 호출되지 않으므로
            //    여기서만 stub을 선언한다.
            given(mockUserDetails.getUserId()).willReturn(1L);
            willDoNothing().given(authService).logout(1L);

            mvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent());

            then(authService).should().logout(1L);
        }

        @Test
        @DisplayName("[성공] 비로그인 상태 → authService.logout 미호출 + 204")
        void 정상_비로그인_서비스미호출() throws Exception {
            mvc.perform(post("/api/auth/logout").header("X-No-Auth", "true"))
                    .andExpect(status().isNoContent());

            then(authService).should(never()).logout(anyLong());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 아이디 찾기 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("아이디 찾기 API [POST /api/auth/find-id]")
    class FindIdApi {

        @Test
        @DisplayName("[성공] 일치하는 정보 → 마스킹 이메일 반환 200")
        void 정상_200() throws Exception {
            given(authService.findId(any(FindIdRequest.class)))
                    .willReturn(FindIdResponse.builder()
                            .maskedEmail("te**@bnk.com")
                            .message("이메일 조회 완료")
                            .build());

            mvc.perform(post("/api/auth/find-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"홍길동\",\"phone\":\"01012345678\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maskedEmail").value("te**@bnk.com"));
        }

        @Test
        @DisplayName("[실패] 가입 정보 없음 → 404 + code=U001")
        void 실패_가입자정보없음_404() throws Exception {
            given(authService.findId(any()))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            mvc.perform(post("/api/auth/find-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"임꺽정\",\"phone\":\"01099998888\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 비밀번호 재설정 링크 요청 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정 링크 요청 API [POST /api/auth/find-password]")
    class FindPasswordApi {

        @Test
        @DisplayName("[성공] 계정 존재 → 링크 발송 200")
        void 정상_200() throws Exception {
            willDoNothing().given(authService).findPassword(any(FindPasswordRequest.class));

            mvc.perform(post("/api/auth/find-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"홍길동\",\"email\":\"test@bnk.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 정보 불일치 → 404 + code=U001")
        void 실패_정보불일치_404() throws Exception {
            willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                    .given(authService).findPassword(any());

            mvc.perform(post("/api/auth/find-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"틀린이름\",\"email\":\"test@bnk.com\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 비밀번호 재설정 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정 API [POST /api/auth/reset-password]")
    class ResetPasswordApi {

        private static final String TOKEN  = "valid-reset-token";
        private static final String NEW_PW = "NewPassword123!!";

        @Test
        @DisplayName("[성공] 유효 토큰 + 비밀번호 일치 → 200")
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
        void 실패_비밀번호확인불일치_400() throws Exception {
            willThrow(new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH))
                    .given(authService).resetPassword(any());

            mvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + TOKEN + "\",\"newPassword\":\"" + NEW_PW +
                             "\",\"newPasswordConfirm\":\"Different!!\"}"))
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

        @Test
        @DisplayName("[실패] 비밀번호 정책 위반(@Pattern) → 400")
        void 실패_비밀번호정책위반_400() throws Exception {
            mvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + TOKEN +
                             "\",\"newPassword\":\"abcd1234\",\"newPasswordConfirm\":\"abcd1234\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 이메일 인증코드 발송 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증코드 발송 API [POST /api/auth/send-verify-code]")
    class SendVerifyCodeApi {

        @Test
        @DisplayName("[성공] 미가입 이메일 → 발송 200")
        void 정상_200() throws Exception {
            willDoNothing().given(authService).sendVerifyCode(any(SendVerifyCodeRequest.class));

            mvc.perform(post("/api/auth/send-verify-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"new@bnk.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 이미 가입된 이메일 → 409 + code=U002")
        void 실패_중복이메일_409() throws Exception {
            willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
                    .given(authService).sendVerifyCode(any());

            mvc.perform(post("/api/auth/send-verify-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"dup@bnk.com\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("U002"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 이메일 인증코드 검증 API
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증코드 검증 API [POST /api/auth/verify-email]")
    class VerifyEmailApi {

        @Test
        @DisplayName("[성공] 유효 코드 → 인증 완료 200")
        void 정상_200() throws Exception {
            willDoNothing().given(authService).verifyEmail(any(EmailVerifyRequest.class));

            mvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@bnk.com\",\"code\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("[실패] 만료·부적합 인증번호 → 400 + code=U008")
        void 실패_인증번호부적합_400() throws Exception {
            willThrow(new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID))
                    .given(authService).verifyEmail(any());

            mvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@bnk.com\",\"code\":\"000000\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U008"));
        }
    }
}